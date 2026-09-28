---
name: commit
description: 변경사항을 기능 단위로 쪼개서 커밋한다. "커밋해줘", "지금까지 작업 커밋", "커밋 나눠줘", "commit this" 같은 요청에 사용. 한 커밋에 한 기능만 담고, 관련 없는 변경은 별도 커밋으로 분리한다.
---

# 기능 단위 커밋 (Android / MOEMI-FE)

이 저장소는 안드로이드 앱(Kotlin / Jetpack Compose / Gradle Kotlin DSL, 패키지 `com.example.moemi`)이다.

**핵심 규칙: 커밋 하나 = 기능 하나.** 변경 파일이 여러 기능에 걸쳐 있으면 커밋을 나눈다.
`git add .` / `git add -A` 로 전부 담는 커밋은 만들지 않는다.

## 1. 현재 상태 파악

```bash
git status --short
git diff            # 아직 stage 안 된 변경
git diff --cached   # 이미 stage 된 변경
git log --oneline -10   # 기존 메시지 스타일 확인 (있으면 그 스타일을 따른다)
```

## 2. 기능 단위로 묶기

변경사항을 아래 기준으로 그룹핑하고, **커밋하기 전에 사용자에게 "이렇게 N개로 나누겠다"고 목록을 보여준다.**

이 프로젝트의 폴더 구조:

| 경로 (`app/src/main/java/com/example/moemi/` 기준) | 내용 |
| --- | --- |
| `feature/<기능>/` | 화면 단위 코드 (`calendar`, `checklist`, `hazard` …) — Screen + State |
| `data/` | 모델, Repository, 규칙 (API 연동 자리) |
| `ui/theme/` | 색상·타이포·테마 |
| `ui/components/` | 여러 화면이 같이 쓰는 컴포넌트, 아이콘, 탭바 |
| `MainActivity.kt` | 앱 진입점, 탭 전환 |
| `app/src/main/res/` | 문자열, 테마 XML, 아이콘 리소스 |

한 커밋에 묶어도 되는 것:
- 하나의 기능 화면에 필요한 `feature/<기능>/` + 그 기능 전용 `data/` 모델·Repository (수직 슬라이스)
- 그 기능의 테스트 코드 (`app/src/test/`, `app/src/androidTest/`)
- 그 화면을 탭/내비게이션에 연결하는 `MainActivity.kt` 변경
- 그 기능 때문에 생긴 의존성 추가 (`gradle/libs.versions.toml` + `app/build.gradle.kts`)

반드시 나눠야 하는 것:
- 서로 다른 기능 화면 (예: 캘린더 ↔ 체크리스트 ↔ 위험 지도)
- 공통 디자인 기반(`ui/theme/`, `ui/components/`) ↔ 그것을 쓰는 개별 화면
  — 공통 부분을 먼저 커밋하고, 화면은 그 뒤에 기능별로 커밋한다
- 기능 추가 ↔ 기존 코드 리팩터링
- 기능 코드 ↔ 빌드 설정만의 변경 (`minSdk`, AGP/Kotlin 버전, `gradle.properties` 단독 변경)
- 오타/포맷팅 정리는 별도 `style:` 또는 `chore:` 커밋

애매하면 "이 커밋만 되돌렸을 때 앱이 빌드되고 말이 되는가?"로 판단한다.
공통 컴포넌트를 쓰는 화면 커밋이 공통 컴포넌트 커밋보다 앞에 오면 중간 커밋이 빌드되지 않으니 순서에 주의한다.

## 3. 선택적으로 stage 하고 커밋

```bash
git add app/src/main/java/com/example/moemi/feature/calendar/ app/src/main/java/com/example/moemi/data/LifeIndex.kt
git diff --cached --stat        # 의도한 파일만 들어갔는지 확인 (필수)
git commit -F- <<'MSG'
feat: 생활수치 캘린더 화면 구현

- 월·주·일 보기와 날짜별 생활기상지수 상세 카드 추가
- 기상청 API 연동 전까지 예시 데이터 Repository 사용
MSG
```

- 파일 하나에 두 기능이 섞였으면: 가능하면 코드를 먼저 정리하고, 어려우면 사용자에게 알린 뒤 한 커밋으로 묶는다.
  (이 환경에서는 `git add -p` 같은 대화형 명령을 쓸 수 없다.)
- 커밋 사이마다 `git status --short` 로 남은 변경을 다시 확인한다.

## 4. 메시지 형식

```
<type>: <한 줄 요약 — 한국어, 50자 내외, 명사형 종결>

- 무엇을 왜 바꿨는지 1~3줄 (어떻게는 코드가 말한다)
```

`Co-Authored-By: Claude ...` 같은 작업자 표기는 넣지 않는다.

type: `feat`(기능) `fix`(버그) `refactor` `test` `chore`(빌드·설정) `docs` `style`(포맷팅·UI 무관 정리) `design`(색상·간격 등 디자인만 조정)

예시:
- `feat: 외출 준비물 체크리스트 화면 구현`
- `fix: 지도 탭에서 필터 변경 시 선택 핀이 사라지는 문제 수정`
- `design: 캘린더 날짜 셀 간격을 시안에 맞춤`
- `chore: minSdk 26으로 상향`

한 줄 요약에 "및", "그리고"가 들어가면 커밋을 나눠야 한다는 신호다.

## 5. 커밋 전 체크리스트

- [ ] `git diff --cached` 에 의도하지 않은 파일이 없는가
  - `build/`, `app/build/`, `.gradle/`, `local.properties`, `*.iml`, `.be-cache/` 는 커밋하지 않는다
  - `.idea/` 는 `.gitignore` 에 일부만 제외돼 있다. `.idea/` 파일은 사용자가 원할 때만 커밋한다
- [ ] API 키·토큰(공공데이터포털 서비스키, 지도 SDK 키 등)이 코드나 `gradle.properties` 에 하드코딩되지 않았는가
  — 키는 `local.properties` 등 커밋되지 않는 곳에 두고 `BuildConfig` 로 주입한다
- [ ] 디버그용 `println`/`Log.d`, 주석 처리한 코드가 섞여 있지 않은가
- [ ] 빌드되는 상태인가 (Windows Git Bash 기준):
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew :app:assembleDebug --console=plain
  ```
  테스트를 건드렸으면 `./gradlew :app:testDebugUnitTest` 도 실행한다.

## 주의

- **push 는 사용자가 명시적으로 요청할 때만** 한다. 커밋까지만 하고 멈춘다.
- 현재 로컬 브랜치는 `master` 이고, 원격 `origin` 은 `wakeupjunyoung/MOEMI-FE` 다.
  **2026-09-28 기준 원격 레포는 비어 있다(브랜치 없음).** push 할 때 대상 브랜치(`main` 권장)를 사용자에게 확인한다.
- 이 저장소는 아직 커밋이 없다. 첫 커밋은 Android Studio 스캐폴드
  (`settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `gradlew*`, `gradle.properties`, `.gitignore`,
  `app/build.gradle.kts`, `app/.gitignore`, `AndroidManifest.xml`, `res/`, 기본 테스트 파일)를
  `chore: Android 프로젝트 초기 설정` 하나로 묶고, 그 뒤 작업부터 기능 단위로 나눈다.
  스캐폴드 이후 수정된 파일(`MainActivity.kt`, `ui/theme/*`)은 최종 상태로 들어가게 되므로,
  첫 커밋이 단독으로 빌드되는지 신경 쓰고 필요하면 사용자와 상의한다.
- `.claude/` 스킬 파일은 기능 코드와 섞지 말고 `chore: Claude 스킬 추가` 처럼 별도 커밋한다.
- `git commit --amend`, `git reset`, `git rebase` 등 히스토리를 바꾸는 명령은 사용자가 요청할 때만 쓴다.
