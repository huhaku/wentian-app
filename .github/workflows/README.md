# GitHub Actions CI/CD 工作流说明

## 功能特性

- ✅ 自动构建 Debug 和 Release APK
- ✅ 在每次 push 到 master 分支时触发构建
- ✅ 在 Pull Request 时触发构建
- ✅ 在创建 tag（v*）时自动发布到 GitHub Releases
- ✅ 构建产物自动上传

## 使用方法

### 1. 自动构建

每次推送到 `master` 分支或创建 Pull Request 时，CI 会自动：
- 构建 Debug APK
- 构建 Release APK（未签名）
- 上传构建产物到 GitHub Actions

### 2. 发布新版本

创建并推送 tag 来触发自动发布：

```bash
# 创建 tag
git tag v1.0.0

# 推送 tag
git push origin v1.0.0
```

CI 会自动：
- 构建 APK
- 创建 GitHub Release
- 上传 APK 到 Release

### 3. 下载构建产物

#### 从 Actions 下载
1. 进入 GitHub 仓库的 "Actions" 页面
2. 选择对应的 workflow 运行记录
3. 在 "Artifacts" 部分下载 APK

#### 从 Releases 下载
1. 进入 GitHub 仓库的 "Releases" 页面
2. 找到对应的版本
3. 下载 APK 文件

## 配置签名（可选）

如果需要对 Release APK 进行签名，需要配置以下 GitHub Secrets：

1. 在 GitHub 仓库设置中添加 Secrets：
   - `KEYSTORE_FILE`：Base64 编码的密钥库文件
   - `KEYSTORE_PASSWORD`：密钥库密码
   - `KEY_ALIAS`：密钥别名
   - `KEY_PASSWORD`：密钥密码

2. 生成 Base64 编码的密钥库：
```bash
base64 -i your-keystore.jks -o keystore.txt
```

3. 将 `keystore.txt` 的内容复制到 `KEYSTORE_FILE` Secret

## 工作流文件

- `.github/workflows/android-ci.yml` - 主要的 CI/CD 工作流配置