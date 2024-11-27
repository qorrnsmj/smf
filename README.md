[SMF]
- 種類ごとにシェーダー使い分けできる？
- stbも自分で実装できる？
- kotlin-mathも自分で実装
- ミップマップ実装
- MasterRendererのsetUniformはprivateにする。
- renderer.start() ~ stop()の間でそのシェーダーの処理はすべて行う。(Uniformの設定とかも)

[Performance]
- Whenever you can reduce the number of texture/vbo/vao binds, 
or uniform loads, it's almost always a good idea to do it.
- 並列処理でロード時間とか減らす

[Fix]
- GL33 -> GL33Cに変更
- MemoryUtil.memFree()できてない場所を直す (lwjglx-debugで検出)
- 全部MemoryStackでできる所は変える (スコープ内でしかメモリ割り当てされないから、メモリ使い捨てに便利)
- フルスクに変えたときのglViewportができてない
- https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/appendixa/appendixa.html
このサイトの家のglDrawElementsの部分見てみたらわかるけど、lowPolyTreeが1296なの多くない？

[Refactor]
- コメントの最初の英単語は大文字の三単現で
- 継承よりも委譲、インターフェース
- check require assert(VMオプションで切り替え可) の3つを使う (ちなみにkotlinには検査例外がないので注意)
- アノテーションで呼び出せるクラスを限定する

[Test-Lighting]
- 法線マッピングの実装 (gimpのプラグインとかある)
- Per-Pixel-Lightingの実装
- 様々な種類の光源を実装 [Light casters](https://learnopengl.com/Lighting/Light-casters)
- 全ての種類の光源を一つに [Multiple lights](https://learnopengl.com/Lighting/Multiple-lights)
- マテリアルの実装
 
[Test-Shading]
- 影modのシェーダー参考に

[Test-Stencil]
- [depth stencil](https://open.gl/depthstencils)
- [ステンシルバッファを使った立体視](https://marina.sys.wakayama-u.ac.jp/~tokoi/?date=20040208)

[Ship]
- 特定のルートを巡行するように
- 画面左上に1, 2, 3, 4, 5のキー操作説明
- FPS表示
- 昼-夕方-夜の切り替え、船上のプレイヤー操作の切り替え、人々の切り替えなどを数字キーで
- 右上にマップを出して船の現在位置表示
- 港も作る
- カモメ飛ばす
- 船のモデル小さすぎるから20倍くらいにする

[狼と若草色の寄り道]
- 絵コンテ書く (紙で, 写真で, 3Dモデルで)
- モデルのアニメーションは一まとまりでBlenderで作成 (fbx, gltf or other)
- 例えば馬車が揺れたら、そこに乗ってるオブジェクトも一緒に揺れるように、model行列を共有しとく (まとまりObject)
- アニメーションを合成して再生できるように
- 服とか布の当たり判定 (ウマ娘, 原神, ゼンゼロ)
- 表情の変化はどうする？
- システム内でレンダリングして動画ファイルに出力するのもあり(4k/60fps?)
- 台詞をクリックで進める or 自動で進むかの設定
- 声の合成がめちゃくちゃ上手くいったら、声ありでもいいかも

[Misc]
- RenderDocでデバッグ[Appendix A - OpenGL Debugging](https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/appendixa/appendixa.html)
- [WebGLで複数のシェーダー使用時にハマる罠](https://qiita.com/emadurandal/items/5966c8374f03d4de3266)
- マイクラのmodは特に参考に[15+ Client Side Mods](https://youtu.be/TWNQVdtMBIE?si=0D31khSS4csT_uZx)
- 加算ブレンド、乗算ブレンド、逆乗算ブレンド使い分ける
- 不透明テクスチャを描画する時に、描画順を気にしたくない時はOrder-Independent-Transparencyを使う
