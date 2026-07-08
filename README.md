# OClock Bell Android

현재 버전: `2.0.0`

`oclock-bell-android`는 정시 알림 웹앱을 안드로이드에서 감싸는 래퍼 앱입니다.

웹 UI는 `WebView`로 띄우고, 다음 기능은 안드로이드 네이티브 코드가 담당합니다.

- 정확한 알람 예약
- 백그라운드 TTS 재생
- 재부팅 후 알람 복구
- 알람 성격의 오디오 동작

## 연결된 웹 저장소

이 안드로이드 프로젝트는 아래 정시 알림 웹 프로젝트와 연결되어 있습니다.

- 웹 저장소: https://github.com/ohsoou/oclock-bell.git

안드로이드 앱은 배포된 웹앱 URL을 `WebView`로 열고, 알람 관련 동작은 `window.NativeAlarm` 브리지를 통해 네이티브 코드와 연결합니다.

## 주요 구성

- `MainActivity.kt`
  웹앱을 `WebView`로 로드합니다.
- `WebAppInterface.kt`
  자바스크립트에서 네이티브 알람 기능을 호출할 수 있게 합니다.
- `AlarmScheduler.kt`
  `AlarmManager`로 정확한 다음 알람을 예약합니다.
- `AlarmReceiver.kt`
  알람 브로드캐스트를 수신하고 서비스 실행을 시작합니다.
- `AlarmService.kt`
  백그라운드에서 한국어 TTS를 재생합니다. 알람 스트림(`USAGE_ALARM`)으로 재생하여 무음/진동 모드와 무관하게 소리가 납니다.
- `TtsSupport.kt`
  저장된 음높이/속도/음량 설정을 TTS 엔진에 적용합니다.
- `AlarmDefaults.kt`
  알람 기본 시각(시작 8시 / 종료 22시)을 한곳에서 관리하며 웹앱 `constants.js`와 값을 맞춥니다.
- `BootReceiver.kt`
  기기 재부팅 후 알람 예약을 복구합니다.

## 빌드 모드

웹 URL은 빌드 타입에 따라 분리되어 있습니다.

- `debug`: `gradle.properties`의 `debugWebAppUrl` 사용
- `release`: `gradle.properties`의 `releaseWebAppUrl` 사용

현재 기본값:

```properties
debugWebAppUrl=http://192.168.0.71:3000/
releaseWebAppUrl=https://oclock-bell.netlify.app/
```

## 로컬 테스트

1. 웹앱 개발 서버를 `3000` 포트에서 실행합니다.
2. 안드로이드 기기에서 `debugWebAppUrl`에 접근 가능한지 확인합니다.
3. Android Studio에서 이 프로젝트를 엽니다.
4. Gradle JDK를 `17`로 설정합니다.
5. 실기기에서 `debug` 빌드를 실행합니다.

다른 네트워크나 다른 장비에서 테스트할 때는 `debugWebAppUrl` 값을 환경에 맞게 변경하면 됩니다.

## 릴리즈 빌드 / 서명

설치 가능한 서명된 릴리즈 APK를 만들려면 프로젝트 루트에 `keystore.properties`가 필요합니다. 이 파일과 키스토어(`*.jks`)는 비밀 정보이므로 `.gitignore`로 커밋에서 제외됩니다.

```properties
# keystore.properties (커밋 금지)
storeFile=oclock-release.jks
storePassword=<스토어 비밀번호>
keyAlias=<키 별칭>
keyPassword=<키 비밀번호>
```

키스토어가 없으면 새로 생성합니다.

```bash
keytool -genkeypair -v \
  -keystore oclock-release.jks -alias oclock \
  -keyalg RSA -keysize 2048 -validity 10000
```

빌드:

```bash
./gradlew assembleRelease
# 결과물: app/build/outputs/apk/release/app-release.apk
```

`keystore.properties`가 없으면 서명 설정이 비활성화되며, 이 경우 `assembleRelease`는 설치 불가한 미서명 APK를 만듭니다. **키스토어와 비밀번호는 분실하면 이후 업데이트를 서명할 수 없으므로 안전하게 보관하세요.**

## 참고 사항

- `local.properties`는 Git에 포함되지 않습니다.
- `debug` 빌드는 로컬 개발을 위해 HTTP를 허용합니다.
- `release` 빌드는 배포된 HTTPS 웹앱 URL을 사용합니다.
