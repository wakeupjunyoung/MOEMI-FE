---
name: pr
description: WSL의 gh CLI로 이 저장소(wakeupjunyoung/MOEMI-FE, 안드로이드 앱)에 Pull Request를 만든다. PR 생성 전에 반드시 최신 base 브랜치를 pull 받아 충돌을 먼저 해결한다. "PR 만들어줘", "풀리퀘 올려줘", "PR 생성", "create a PR" 같은 요청에 사용.
---

# PR 생성 (gh CLI)

- 저장소: `wakeupjunyoung/MOEMI-FE` (안드로이드 앱) / gh 계정 `jahan-93`
- 기본 base 브랜치: `main`
- **이 스킬은 사용자가 PR을 요청했을 때만 동작한다.**
  push 직전에 "`<브랜치>` 를 origin에 push 하고 PR을 만들겠다"고 알리고 진행한다.
- `gh` 는 Windows(Git Bash) PATH에 없고 **WSL에만 있다.** gh 명령은 `wsl.exe -e gh ...` 로 실행한다.
  git 명령은 Git Bash 에서 그대로 실행한다.

## 0. 원격이 비어 있는 경우 (최초 1회)

2026-09-28 기준 MOEMI-FE 원격에는 브랜치가 하나도 없다. 이 상태에서는 PR을 만들 수 없으므로 먼저 확인한다.

```bash
git ls-remote --heads origin
```

- 출력이 비어 있으면: PR 대신 **base 브랜치(`main`)를 먼저 올려야 한다.** 사용자에게 알리고 승인받은 뒤
  초기 설정 커밋만 담아 `main` 을 만든다.
  ```bash
  git push origin <초기설정-커밋-해시>:refs/heads/main
  ```
  그 다음 기능 브랜치를 `main` 위로 옮겨 1번부터 진행한다.
- 어떤 커밋까지를 `main` 에 올릴지는 반드시 사용자에게 확인한다. 임의로 전부 올리지 않는다.

## 1. 사전 점검

```bash
wsl.exe -e gh auth status        # 로그인/스코프 확인 (repo 스코프 필요)
git status --short               # 커밋 안 된 변경이 남아 있는지
git branch --show-current
git fetch --prune origin
git log --oneline origin/main..HEAD   # PR에 담길 커밋 목록
```

- 커밋되지 않은 변경이 있으면 멈추고 사용자에게 알린다. `commit` 스킬로 기능 단위 커밋을 먼저 만든다.
- 현재 브랜치가 `main`/`master`면 PR을 만들 수 없다. 기능 브랜치를 먼저 만든다:
  `git switch -c feat/<기능명>` (예: `feat/calendar`, `feat/checklist`, `feat/hazard-map`)
  기존 커밋을 옮겨야 하면 사용자에게 확인한다.
- `origin/main..HEAD` 가 비어 있으면 PR로 올릴 커밋이 없는 것이다. 멈춘다.

## 2. base 브랜치 pull (필수)

PR을 만들기 **전에** 항상 최신 base를 받아 온다.

```bash
git pull --rebase origin main     # 현재 기능 브랜치 위로 최신 main 을 올린다
```

- 충돌이 나면 **PR 생성을 중단**하고 충돌 파일 목록과 함께 사용자에게 보고한다.
  임의로 한쪽을 버리고 해결하지 않는다. 해결 후 `git rebase --continue`.
  - `gradle/libs.versions.toml`, `app/build.gradle.kts` 충돌은 버전을 합치는 경우가 많으니 양쪽 의도를 설명하고 확인받는다.
  - `.idea/` 파일 충돌은 커밋 대상인지부터 사용자에게 묻는다.
- rebase 가 곤란한 상황(이미 공유된 브랜치 등)이면 `git merge origin/main` 으로 대체하고 그 사실을 알린다.
- pull 이후 빌드가 깨지지 않는지 확인한다:
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew :app:assembleDebug --console=plain
  ```
  테스트를 건드렸으면 `./gradlew :app:testDebugUnitTest` 도 실행한다.

## 3. push + PR 생성

```bash
git push -u origin "$(git branch --show-current)"
```

PR 본문은 파일로 만든 뒤 WSL gh 에 넘긴다 (Windows 경로는 WSL에서 `/mnt/c/...` 로 보인다).

```bash
BRANCH="$(git branch --show-current)"
cat > .git/PR_BODY.md <<'BODY'
## 요약
생활수치 캘린더 화면(기능 1)을 추가했다.

## 변경사항
- 월·주·일 보기와 날짜별 생활기상지수 상세 카드
- 지역 선택, 새로고침, 마지막 갱신 시각 표시
- 기상청 API 연동 전까지 예시 데이터 Repository 사용

## 스크린샷
<!-- 에뮬레이터/Preview 캡처를 첨부 -->

## 테스트
- `./gradlew :app:assembleDebug` 통과
- 에뮬레이터에서 날짜 선택·보기 전환 수동 확인

## 확인 필요
- 기상청 API 서비스키 관리 방식 (local.properties → BuildConfig)
BODY

wsl.exe -e gh pr create --repo wakeupjunyoung/MOEMI-FE --base main --head "$BRANCH" \
  --title "feat: 생활수치 캘린더 화면 구현" \
  --body-file "/mnt/c/Users/t0108/Desktop/all/android1/.git/PR_BODY.md"
rm .git/PR_BODY.md
```

- PR 본문에 Claude 생성 표기("Generated with Claude Code" 등)를 넣지 않는다.
- 제목은 대표 커밋 메시지와 같은 형식(`<type>: <한국어 요약>`)을 쓴다.
- 커밋이 여러 개면 제목은 PR 전체를 아우르는 한 줄로 쓰고, 세부는 `## 변경사항`에 나열한다.
- UI 변경 PR이면 `## 스크린샷` 섹션을 남기고, 캡처가 없으면 "스크린샷 없음 — Preview로 확인 필요"라고 적는다.
  관련 디자인 시안(lifehelper_svg 파일명)이나 노션 기능명세서 기능 번호를 요약에 함께 적는다.
- 초안으로 올릴 때는 `--draft` 를 붙인다.
- 생성 후 PR URL을 사용자에게 그대로 전달한다.

## 4. 실패 시

- `gh` 인증 만료: `gh auth login` 은 대화형이라 이 환경에서 실행할 수 없다.
  사용자에게 WSL 터미널에서 `gh auth login` 을 직접 실행하도록 안내한다.
- `git push` 인증 실패: Git Bash 의 자격 증명(Git Credential Manager)이 필요하다. 사용자에게 알리고 멈춘다.
- push 거부(non-fast-forward): 강제 push 하지 말고 2번의 pull 단계를 다시 수행한 뒤 보고한다.
- PR이 이미 존재하면 `wsl.exe -e gh pr view --repo wakeupjunyoung/MOEMI-FE "$BRANCH"` 로 기존 PR을 보여주고,
  새로 만들지 말고 push 만으로 갱신한다.

## 금지

- `git push --force`, `--force-with-lease` 는 사용자가 명시적으로 요청할 때만.
- PR 머지(`gh pr merge`)는 이 스킬의 범위가 아니다. 사용자가 따로 요청해야 한다.
- base 브랜치(`main`)에 직접 push 하지 않는다. (0번의 최초 `main` 생성만 예외이며, 그때도 사용자 승인 후 진행)
- 다른 저장소(MOEMI-BE 등)에 PR을 만들지 않는다. 백엔드 변경이 필요하면 내용만 정리해 사용자에게 전달한다.
