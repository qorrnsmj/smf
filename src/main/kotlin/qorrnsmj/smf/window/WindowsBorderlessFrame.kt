package qorrnsmj.smf.window

import com.sun.jna.CallbackReference
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.win32.StdCallLibrary.StdCallCallback
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.system.Platform

internal class WindowsBorderlessFrame(private val glfwWindow: Long) {
    private val user32 = Native.load("user32", User32::class.java)
    private var handle: Pointer? = null
    private var previousWindowProc: Pointer? = null
    private var callback: WindowProc? = null

    fun install() {
        if (Platform.get() != Platform.WINDOWS || handle != null) return

        val windowHandle = Pointer(glfwGetWin32Window(glfwWindow))
        val windowProc = WindowProc { hwnd, message, wParam, lParam ->
            when (message) {
                WM_NCCALCSIZE -> calculateClientArea(hwnd, wParam, lParam)
                WM_NCHITTEST -> resizeHitTest(hwnd, lParam).takeIf { it != HTCLIENT }
                    ?: callPrevious(hwnd, message, wParam, lParam)
                else -> callPrevious(hwnd, message, wParam, lParam)
            }
        }
        val procPointer = CallbackReference.getFunctionPointer(windowProc)
        val previous = user32.SetWindowLongPtrW(windowHandle, GWLP_WNDPROC, Pointer.nativeValue(procPointer))
        check(previous != 0L) { "Failed to subclass the Editor window" }

        handle = windowHandle
        callback = windowProc
        previousWindowProc = Pointer(previous)

        val style = user32.GetWindowLongPtrW(windowHandle, GWL_STYLE)
        val borderlessStyle = (style and WS_CAPTION.inv()) or
            WS_THICKFRAME or WS_MAXIMIZEBOX or WS_MINIMIZEBOX or WS_SYSMENU
        user32.SetWindowLongPtrW(windowHandle, GWL_STYLE, borderlessStyle)
        user32.SetWindowPos(
            windowHandle,
            null,
            0,
            0,
            0,
            0,
            SWP_FRAMECHANGED or SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_NOACTIVATE,
        )
    }

    fun beginMove() {
        val windowHandle = handle ?: return
        user32.ReleaseCapture()
        user32.SendMessageW(windowHandle, WM_NCLBUTTONDOWN, HTCAPTION.toLong(), 0L)
    }

    private fun calculateClientArea(windowHandle: Pointer, wParam: Long, lParam: Long): Long {
        if (wParam == 0L) return callPrevious(windowHandle, WM_NCCALCSIZE, wParam, lParam)
        if (!user32.IsZoomed(windowHandle)) return 0L

        val monitor = user32.MonitorFromWindow(windowHandle, MONITOR_DEFAULTTONEAREST) ?: return 0L
        val monitorInfo = MonitorInfo()
        if (!user32.GetMonitorInfoW(monitor, monitorInfo)) return 0L

        val clientRect = Rect(Pointer(lParam))
        clientRect.read()
        clientRect.left = monitorInfo.work.left
        clientRect.top = monitorInfo.work.top
        clientRect.right = monitorInfo.work.right
        clientRect.bottom = monitorInfo.work.bottom
        clientRect.write()
        return 0L
    }

    private fun resizeHitTest(windowHandle: Pointer, lParam: Long): Long {
        if (user32.IsZoomed(windowHandle)) return HTCLIENT
        val rect = Rect()
        if (!user32.GetWindowRect(windowHandle, rect)) return HTCLIENT
        val x = lParam.toInt().toShort().toInt()
        val y = (lParam shr 16).toInt().toShort().toInt()
        val border = (RESIZE_BORDER * user32.GetDpiForWindow(windowHandle) / DEFAULT_DPI).coerceAtLeast(RESIZE_BORDER)
        val left = x < rect.left + border
        val right = x >= rect.right - border
        val top = y < rect.top + border
        val bottom = y >= rect.bottom - border
        return when {
            top && left -> HTTOPLEFT
            top && right -> HTTOPRIGHT
            bottom && left -> HTBOTTOMLEFT
            bottom && right -> HTBOTTOMRIGHT
            left -> HTLEFT
            right -> HTRIGHT
            top -> HTTOP
            bottom -> HTBOTTOM
            else -> HTCLIENT
        }
    }

    private fun callPrevious(hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long {
        val previous = previousWindowProc ?: return user32.DefWindowProcW(hwnd, message, wParam, lParam)
        return user32.CallWindowProcW(previous, hwnd, message, wParam, lParam)
    }

    private fun interface WindowProc : StdCallCallback {
        fun invoke(hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
    }

    private interface User32 : Library {
        fun GetWindowLongPtrW(hwnd: Pointer, index: Int): Long
        fun SetWindowLongPtrW(hwnd: Pointer, index: Int, value: Long): Long
        fun SetWindowPos(hwnd: Pointer, insertAfter: Pointer?, x: Int, y: Int, width: Int, height: Int, flags: Int): Boolean
        fun GetWindowRect(hwnd: Pointer, rect: Rect): Boolean
        fun IsZoomed(hwnd: Pointer): Boolean
        fun MonitorFromWindow(hwnd: Pointer, flags: Int): Pointer?
        fun GetMonitorInfoW(monitor: Pointer, monitorInfo: MonitorInfo): Boolean
        fun GetDpiForWindow(hwnd: Pointer): Int
        fun CallWindowProcW(previous: Pointer, hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
        fun DefWindowProcW(hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
        fun ReleaseCapture(): Boolean
        fun SendMessageW(hwnd: Pointer, message: Int, wParam: Long, lParam: Long): Long
    }

    @Structure.FieldOrder("left", "top", "right", "bottom")
    class Rect(pointer: Pointer? = null) : Structure(pointer) {
        @JvmField var left = 0
        @JvmField var top = 0
        @JvmField var right = 0
        @JvmField var bottom = 0
    }

    @Structure.FieldOrder("size", "monitor", "work", "flags")
    class MonitorInfo : Structure() {
        @JvmField var size = size()
        @JvmField var monitor = Rect()
        @JvmField var work = Rect()
        @JvmField var flags = 0
    }

    private companion object {
        const val GWL_STYLE = -16
        const val GWLP_WNDPROC = -4
        const val WS_CAPTION = 0x00C00000L
        const val WS_SYSMENU = 0x00080000L
        const val WS_THICKFRAME = 0x00040000L
        const val WS_MINIMIZEBOX = 0x00020000L
        const val WS_MAXIMIZEBOX = 0x00010000L
        const val WM_NCCALCSIZE = 0x0083
        const val WM_NCHITTEST = 0x0084
        const val WM_NCLBUTTONDOWN = 0x00A1
        const val MONITOR_DEFAULTTONEAREST = 0x00000002
        const val HTCLIENT = 1L
        const val HTCAPTION = 2
        const val HTLEFT = 10L
        const val HTRIGHT = 11L
        const val HTTOP = 12L
        const val HTTOPLEFT = 13L
        const val HTTOPRIGHT = 14L
        const val HTBOTTOM = 15L
        const val HTBOTTOMLEFT = 16L
        const val HTBOTTOMRIGHT = 17L
        const val SWP_NOSIZE = 0x0001
        const val SWP_NOMOVE = 0x0002
        const val SWP_NOZORDER = 0x0004
        const val SWP_NOACTIVATE = 0x0010
        const val SWP_FRAMECHANGED = 0x0020
        const val RESIZE_BORDER = 8
        const val DEFAULT_DPI = 96
    }
}
