# InsightFace Recognizer（安卓人脸识别）

基于 **InsightFace 官方 InspireFace SDK**（[deepinsight/insightface](https://github.com/deepinsight/insightface)）的安卓人脸识别应用。选择一张照片，应用会检测图片中的所有人脸，并返回 SDK 提供的**全部**信息（检测框、106 关键点、512 维识别特征、年龄、性别、人种、口罩、图像质量、嘴巴/眼睛状态），同时通过本地 **FeatureHub** 向量库进行 1:N 识别，判断“图片中的人是谁”。

## 技术栈

- **Kotlin + Jetpack Compose + Material 3**
- **InspireFace Android SDK 1.2.0**（JitPack：`com.github.HyperInspire:inspireface-android-sdk:1.2.0`）
  - 官方 C/C++ 跨平台 SDK 的安卓发布，模型包（Pikachu / Megatron）已内置在 AAR 中，`GlobalLaunch` 时自动解包，无需手动下载模型。
- minSdk 24 / targetSdk 35，ABI：arm64-v8a、armeabi-v7a

## 对接的官方 API（均来自官方示例验证）

| 能力 | 官方 API |
|------|----------|
| 引擎启动/终止 | `InspireFace.GlobalLaunch` / `GlobalTerminate` |
| 会话创建 | `InspireFace.CreateCustomParameter` + `CreateSession`（`DETECT_MODE_ALWAYS_DETECT`） |
| 人脸检测 | `CreateImageStreamFromBitmap` → `ExecuteFaceTrack`（返回 `MultipleFaceData`） |
| 特征提取 | `InspireFace.ExtractFaceFeature` |
| 关键点 | `InspireFace.GetFaceDenseLandmarkFromFaceToken` |
| 对齐裁剪 | `InspireFace.GetFaceAlignmentImage` |
| 属性流水线 | `MultipleFacePipelineProcess` + `GetFaceMaskConfidence` / `GetFaceQualityConfidence` / `GetFaceAttributeResult` / `GetFaceInteractionStateResult` / `GetFaceInteractionActionsResult` |
| 人脸库 1:N | `CreateFeatureHubConfiguration` + `FeatureHubDataEnable` + `FeatureHubFaceSearchTopK` |
| 人脸库 CRUD | `FeatureHubInsertFeature` / `FeatureHubFaceUpdate` / `FeatureHubFaceRemove` / `FeatureHubGetFaceIdentity` |

## 功能

1. **人脸识别**：选照片 → 检测 → 1:N 识别 → 展示 SDK 全部返回条目，未识别的人脸可一键注册入库。
2. **人脸库管理**：注册 / 搜索 / 重命名 / 删除已知人脸（特征存于本地 FeatureHub，头像存于应用私有目录）。
3. **多主题**：5 个亮色主题（深海蓝、海洋青、森林绿、日落橙、皇室紫），**无暗色模式**，主题持久化。
4. **版本更新**：启动时通过 GitHub Releases API 检测新版本，自动下载 APK 并唤起系统安装。
5. **自动编译**：GitHub Actions 在每次推送时编译 APK，打 `v*` tag 时自动发布 Release（供应用内更新检查）。

## 使用流程

> InsightFace 只返回人脸特征向量，不能凭空“知道”陌生人是谁。要识别身份，需先注册人脸到本地 FeatureHub：

1. 打开「识别」页，选一张已知人物的照片，点该人脸卡片上的「注册到人脸库」，输入姓名。
2. 在「人脸库」页可查看/重命名/删除已注册人脸。
3. 此后再用「识别」页选任意照片，已注册的人会被标记为绿色框并显示姓名与相似度。

## 构建

需要 JDK 17（AGP 8.6 要求）。

```bash
# 生成 Gradle Wrapper（首次）
gradle wrapper --gradle-version 8.7

# 编译
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

# 安装到设备
./gradlew :app:installDebug
```

或在 Android Studio（Ladybug+）中直接打开。首次同步需联网访问 `google()`、`mavenCentral()` 和 `jitpack.io`。

## 配置更新源

应用内更新检查会访问 `https://api.github.com/repos/<owner>/<repo>/releases/latest`。在你的 fork 根目录 `gradle.properties` 中设置：

```properties
GITHUB_REPO_OWNER=你的GitHub用户名
GITHUB_REPO_NAME=InsightFaceRecognizer
```

发布新版本：`git tag v1.1.0 && git push origin v1.1.0` —— GitHub Actions 会自动编译并发布 Release（含 `.apk`），应用下次启动即可检测到更新。

## 目录结构

```
app/src/main/kotlin/com/insightface/recognizer/
├── App.kt / MainActivity.kt
├── data/            # FaceEngine / FaceAnalyzer / FaceRepository / FaceManager（官方 API 封装）
├── update/          # UpdateChecker（GitHub Releases）+ AppUpdateManager（下载安装）
└── ui/
    ├── theme/       # 5 个亮色主题 + ThemeManager
    ├── nav/         # 底部导航
    ├── home/        # 首页
    ├── recognize/   # 人脸识别（核心）
    ├── manage/      # 人脸库管理
    ├── settings/    # 主题/模型/版本设置
    └── components/  # 更新对话框
```

## 许可

InspireFace 开源模型仅供学术研究使用，禁止商业用途。应用代码本身可按需使用。
