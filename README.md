# Билимдон — Android илова (APK)

Илова ичида `app/src/main/assets/index.html` бор. Бу бутун илова: дизайн, тестлар, тўловлар ва админ панели шу файлда.

## APK олишнинг 2 та йўли

### 1-йўл: Android Studio (компьютерда)
1. https://developer.android.com/studio дан Android Studio'ни ўрнатинг.
2. **File → Open** → `BilimdonApp` папкасини танланг. Gradle ўзи юкланади (5–10 дақиқа).
3. **Build → Build App Bundle(s)/APK(s) → Build APK(s)**.
4. Тайёр файл: `app/build/outputs/apk/debug/app-debug.apk`. Уни телефонга ташлаб ўрнатинг.

### 2-йўл: GitHub (компьютерсиз ҳам бўлади)
1. github.com да янги репозиторий очинг ва бу папкадаги барча файлларни юкланг.
2. **Actions** бўлимида «APK йиғиш» ўзи ишга тушади (3–5 дақиқа).
3. Тайёр бўлгач **Artifacts → Bilimdon-APK** ни юклаб олинг.

## Play Market учун
**Build → Generate Signed Bundle / APK** орқали ўз калитингиз (keystore) билан имзоланг. Калитни ҳеч қачон йўқотманг.

## Савол ёки нархни ўзгартириш
Илова ичидаги админ панелдан ўзгартирилади (демо пароль: `admin123`, дарҳол алмаштиринг).
