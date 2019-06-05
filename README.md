


<p>前段时间在找代码片段的管理工具, 找不到满意的, 遂决定自己做一个. 因为在编码的过程中时常发现某个功能以前做过了, 想去找原来的代码, 但是原来的代码早不知道扔哪去了, 所以我希望有个工具能帮我管理这些代码片段, 以便需要的时候可以快速找到它们.</p>
<p> </p>
<p>其实代码复用的最好方式应该是组件化, 但是有些代码, 或是配置之类的东西没法组件化, 只能以片段的形式存在, 于是找到它们变成一个头痛的问题. 两年前做过一个类似的东西, 只是功能弱了点, 我自己也很少用, 所以这个算是2.0版本了.</p>
<p> </p>
<p><br><br><img src="http://dl2.iteye.com/upload/attachment/0086/1037/ba7dcb61-0eb6-3aaf-b0ee-c1cc8a66afa0.png" alt=""><br>  </p>
<p><br><img src="http://dl2.iteye.com/upload/attachment/0086/1035/e36f58ff-ab8d-3e9f-9eca-742bfca3d16d.png" alt=""><br> </p>
<p>主要功能:</p>
<ul>
<li>实时快速查找(类似 eclipse 里 preferences 的 filter, 支持多线程, 10万条上限为10k的随机字符数据查找不超过2秒, 我的cpu是 i5-2430m), 按 esc 可以在任何位置快速复位</li>
<li>查找结果将按照关键字匹配程度由高到低排列</li>
<li>支持代码框内的正则搜索</li>
<li>支持代码折叠, 以及三十多种语言的语法高亮, 语法高亮方案将参考第一个关键字, 具体支持哪些语言呢, 有兴趣的朋友自己去试试吧</li>
<li>代码编辑框可以支持 eclipse 的一些快捷键, 貌似可以把它当代码编辑器来用</li>
<li>支持给每个片段条目加附件, 附件可以单击, 在内置的浏览器(WebKit核心)中打开, 或者双击打开附件文件</li>
<li>支持把某些片段条目导出为带语法高亮的 html 文件, 这样也方便交流.</li>
<li>保存的 zcl2 库文件有压缩处理, 因为文本的压缩潜力很大</li>
<li>功能提示都会在状态栏或 tooltips 里出现, 更多功能可以慢慢挖掘, 发现彩蛋是件令人兴奋的事.</li>
<li>支持 Markdown (md/markdown)</li>
<li>支持 PlantUML (puml)</li>
</ul>
<p> </p>
<p> </p>
<p>导出的 html:</p>
<p><br><img src="http://dl.iteye.com/upload/attachment/0083/8838/64144baf-f126-3c2a-9c64-3a0ef9ac9537.png" alt=""><br> </p>
<p> </p>
<p> </p>
<p> </p>
<p>Thanks to</p>
<ul>
<li>Minimal Icons (icon resources)</li>
<li>Fifesoft (RSyntaxTextArea)</li>
<li>CodeMirror (HTML Highlight)</li>
<li>flexmark-java</li>
<li>PlantUML</li>
</ul>
<p> </p>
</div>
