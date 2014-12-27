junit-seasar2
=============

JUnit4.4 よりも新しい JUnit4 をベースにした Seasar2 のテストランナーです。
そのため `@Rule` などを使用できます。

### 使い方

`@RunWith` で指定している `Seasar2.class` を `Seasar24.class` に変更するだけです。

```Java
@RunWith(Seasar24.class)
public class FooTest {
    // ...
}
```

### 未実装

基本的に Seasar2 からコピペしているだけなので、以前と同じように動作しますが、
以下の機能はまだ実装(コピペ)していません。

* `@Mock`
* Warm Deploy

他にもあるかもしれません m(__)m
