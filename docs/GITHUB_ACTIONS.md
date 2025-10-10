# GitHub Actions: Automated Release Builds

## Overview

The project is configured to build releases automatically using GitHub Actions. The workflow file is located at: `.github/workflows/release.yml`

## When the workflow runs

1. On version tag creation: When you push a tag starting with `v` (e.g., `v1.2.0`)
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

2. Manually: Via the GitHub UI under Actions → Build and Release → Run workflow

## What the workflow does

The workflow launches three parallel jobs to create distributions:

### 1. macOS (DMG)
- Runners: `macos-14` (arm64) and `macos-13` (x64)
- Installs JDK 25 (Temurin)
- Builds DMGs using Maven profiles `-Pmac-aarch64` and `-Pmac-x64`
- Result: `family-tree-editor-{version}-macos*.dmg`
- If secrets are configured (see below), the artifact will be automatically code signed (codesign), submitted for notarization (notarytool), and stapled to avoid Gatekeeper warnings

### 2. Windows (MSI)
- Runner: `windows-latest`
- Installs JDK 25 (Temurin)
- Builds MSI using Maven profile `-Pwindows`
- Result: `family-tree-editor-{version}-windows.msi`
- Preinstalled: WiX Toolset v3.x (available on GitHub runners)

### 3. Linux (DEB)
- Runner: `ubuntu-latest`
- Installs JDK 25 (Temurin)
- Automatically installs: `fakeroot` and `binutils` (required by jpackage)
- Builds DEB using Maven profile `-Plinux`
- Result: `family-tree-editor-{version}-linux-amd64.deb`

### 4. Release creation
- Runs only when a tag is pushed (not on manual runs)
- Downloads all artifacts from all three platforms
- Creates a GitHub Release with:
  - Auto-generated release notes
  - All built files (DMG, MSI, DEB)
  - Not a draft (draft: false)
  - Not a pre-release

## What is required

### In the GitHub repository:

1. Permissions:
   - The workflow is already set with the correct permissions (`contents: write`) at the workflow level
   - GitHub automatically provides `GITHUB_TOKEN` for creating releases

2. Secrets:
   - Not required for basic release creation. `GITHUB_TOKEN` is provided automatically

3. Repository settings:
   - In Settings → Actions → General → Workflow permissions:
     - ✓ "Read and write permissions" (or)
     - ✓ "Read repository contents and packages permissions" plus "Allow GitHub Actions to create and approve pull requests"

### On your local machine (to create a tag):

```bash
# 1. Ensure your code is committed
git add .
git commit -m "Release version 1.2.0"

# 2. Create and push the tag
git tag v1.2.0
git push origin main
git push origin v1.2.0

# The workflow will start automatically!
```

## Checking build status

1. Go to your repository on GitHub
2. Open the **Actions** tab
3. Find the "Build and Release" run
4. Open it to view logs for each job

## Common issues and solutions

### ❌ Issue: JDK 25 unavailable
Solution: GitHub Actions uses Temurin (Eclipse Adoptium), which supports JDK 25. If needed, you can switch the distribution to `oracle` or `zulu`.

### ❌ Issue: Windows build cannot find WiX
Solution: WiX Toolset v3.x is preinstalled on `windows-latest` runners. If issues persist, add explicit installation:
```yaml
- name: Install WiX (if needed)
  run: choco install wixtoolset -y
```

### ❌ Issue: Linux DEB is not created
Solution: The workflow installs `fakeroot` and `binutils` before building. Uploading the DEB artifact is now mandatory (`if-no-files-found: error`), so if the package is not created, the job will fail. Check the jpackage logs and ensure dependencies are installed and the `-Plinux` profile produced a `.deb` in `target/dist/`.

### ❌ Issue: Release not created
Reason: The workflow creates a release only when a tag starting with `v` is pushed.

Solution: Verify you pushed the tag correctly:
```bash
git tag -l  # List local tags
git ls-remote --tags origin  # List tags on GitHub
```

### ❌ Issue: Permission error when creating a release
Solution:
1. Go to Settings → Actions → General
2. Under "Workflow permissions" select "Read and write permissions"
3. Click "Save"

## Manual run (without creating a release)

If you want to test the build without creating a release:

1. Go to Actions → Build and Release
2. Click "Run workflow"
3. Choose the branch
4. Click "Run workflow"

This will produce artifacts but will not create a GitHub Release (releases are created only on tags).

## Artifact structure

After a successful build, the following artifacts will be available:
- `macos-dmg` - contains the DMG file
- `windows-msi` - contains the MSI file
- `linux-deb` - contains the DEB file (if the build succeeded)

Artifacts are retained for 90 days (GitHub default).

## Final checklist for the first release

- [ ] Code committed and pushed to main/master
- [ ] In Settings → Actions → General, permissions set to "Read and write permissions"
- [ ] Version tag created and pushed (e.g., `v1.2.0`)
- [ ] Workflow started automatically (check the Actions tab)
- [ ] All three jobs (macOS, Windows, Linux) completed successfully
- [ ] GitHub Release created automatically with attached files

## Additional notes

### Versioning
The version is taken from `pom.xml` (`<version>1.2.0-SNAPSHOT</version>`).
- Installer tools use only the numeric version (1.2.0)
- The `-SNAPSHOT` suffix is automatically removed by build-helper-maven-plugin
- If the major version is 0, it is automatically replaced with 1 (requirement for macOS/Windows)

### Caching
The workflow uses Maven dependency caching (`cache: 'maven'`) to speed up subsequent builds.

### Parallelism
All three platforms build in parallel, reducing the total build time to about 10–15 minutes (instead of 30–45 minutes sequentially).
