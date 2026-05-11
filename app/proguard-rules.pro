# LastDone proguard rules.
# Compose, AdMob, Room, WorkManager 등 주요 의존성은 consumer rules로 keep 룰이 따라오므로
# 별도 명시 없이 동작하는 게 일반적. 아래는 R8 켜는 첫 단계에서 안전망용 추가 룰.

# kotlinx-coroutines: ServiceLoader/리플렉션 사용
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Room: @Entity 데이터 클래스 보존 (DAO가 reflective access)
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database class * { *; }

# WorkManager: 우리 워커는 클래스명으로 인스턴스화됨
-keep class com.lastdone.app.notification.DailyCheckWorker { <init>(...); }

# BroadcastReceiver / Widget: Manifest 등록분은 R8이 자동 keep 하지만 안전망
-keep class com.lastdone.app.notification.BootReceiver { *; }
-keep class com.lastdone.app.notification.MarkDoneReceiver { *; }
-keep class com.lastdone.app.notification.RepeatAlarmReceiver { *; }
-keep class com.lastdone.app.widget.LastDoneWidgetProvider { *; }
