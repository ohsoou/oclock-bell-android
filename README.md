# OClock Bell Android

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
  백그라운드에서 한국어 TTS를 재생합니다.
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

## 참고 사항

- `local.properties`는 Git에 포함되지 않습니다.
- `debug` 빌드는 로컬 개발을 위해 HTTP를 허용합니다.
- `release` 빌드는 배포된 HTTPS 웹앱 URL을 사용합니다.
