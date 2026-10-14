# ÉTUDIANTS EN DROIT MAROC 🎓⚖️

تطبيق Android (Kotlin) مجاني لطلبة القانون بالمغرب: دروس PDF (أونلاين وأوفلاين)، فيديوهات، منتدى، دردشة، أصدقاء، وإشعارات.

## الميزات
- تسجيل الدخول بـ Google فقط (Firebase Auth)
- صفحة رئيسية بأزرار عريضة: فيديوهات، قانون خاص، قانون عام، بحوث الماستر، بحوث الدكتوراه
- الخاص والعام: الفصول ← المواد ← بطاقات PDF. الفيديوهات: شجرة مستقلة. الماستر والدكتوراه: PDF مباشرة
- عارض PDF مبني على `PdfRenderer` (بلا مكتبات native، متوافق مع 16KB)
- منتدى وتعليقات ودردشة 1-to-1 وإشعارات (FCM)
- إبلاغ وحظر وحذف الحساب
- إعلانات AdMob، وترتيب الصفحة الرئيسية وأماكن الإعلانات كيتحكم فيهم عن بعد
- صفحات HTML تتزاد من لوحة الأدمن

## الهيكل
- `app/`: تطبيق Android (`targetSdk 36`)
- `admin-panel/`: لوحة تحكم ويب (Firebase Auth + Firestore)
- `docs/`: الموقع (GitHub Pages) وسياسة الخصوصية وصفحة حذف الحساب
- `firestore.rules`: قواعد الأمان، كتتنشر أوتوماتيكيا من CI

## البناء والنشر
- GitHub Actions (`.github/workflows/build.yml`): بناء APK debug، ونسخة موقّعة (APK + AAB) كتتنشر فـ release `latest`
- `versionCode` خاص يزيد مع كل نسخة موجهة للمتاجر

## الإعداد
- ملف `app/google-services.json` من مشروع Firebase (package `com.etudiantsdroitmaroc.app`)
- فعّل Google Sign-In وزيد بصمات SHA-1 و SHA-256 (debug وrelease، وبصمة Play App Signing إلا كان النشر على Google Play)
- مفاتيح التوقيع كتبقى فـ GitHub Secrets فقط، ما كتنزلش لـ repo أبدا
