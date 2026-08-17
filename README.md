# 腺动（Kegel Trainer）

面向男性的盆底肌训练 Android 应用。对标 G动的核心训练闭环：个性化 28 天计划、课程库、语音/震动引导播放器、履历与提醒。数据只保存在本机，无需账号。

不是医疗器械，不宣称治疗任何疾病。

## 运行

需要 JDK 17 与 Android SDK（compileSdk 35）。

```bash
cp local.properties.example local.properties
# sdk.dir 请指向 Android Studio 的 SDK（默认 $HOME/Library/Android/sdk）
# 不要用 Homebrew 的 android-commandlinetools，否则和 Studio 调试、模拟器会对不上。

export JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || echo /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home)"

./gradlew :app:installDebug
# 或只编译
./gradlew :app:assembleDebug
# 领域层单测
./gradlew :app:testDebugUnitTest
```

用 Android Studio 打开本目录，选中 `app` 运行即可。

## 使用路径

1. 阅读健康声明并完成问卷（年龄、经验、目标、每日时长）
2. 按教程找对盆底肌，可选做最长保持测试
3. 在「今日」完成当天主课；「课程」可随时加练
4. 「履历」查看月历、连续天数与成就
5. 「我的」开关语音/震动/隐蔽模式，设置提醒，阅读知识库

## 技术

Kotlin + Jetpack Compose + Hilt + Room + DataStore。训练时钟使用 `elapsedRealtime`，避免阶段漂移。
