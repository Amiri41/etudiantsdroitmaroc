# ÉTUDIANTS EN DROIT MAROC 🎓⚖️

تطبيق Native Android لطلبة القانون بالمغرب - قراءة PDFs (أوفلاين وأونلاين)، منتدى، ودردشة مباشرة.

---

## 🏗️ ماذا تم بناؤه

### المرحلة 1
- ✅ هيكلة مشروع Android Kotlin كاملة (MVVM)
- ✅ تسجيل الدخول بـ Google فقط (Firebase Auth)
- ✅ الصفحة الرئيسية: شبكة المواد (قانون خاص / قانون عام)
- ✅ عرض PDFs لكل مادة + تحميل للقراءة أوفلاين (Room DB) + قراءة أونلاين مباشرة
- ✅ منتدى: نشر ومشاهدة منشورات الطلبة (Firestore)
- ✅ دردشة 1-to-1 مباشرة (Real-time) + لائحة المتصلين الآن
- ✅ الأيقونة والألوان (كحل/ذهبي/أخضر) من التصميم اللي بعتيتي
- ✅ GitHub Actions لبناء APK أوتوماتيكيا

### المرحلة 2 (جديد)
- ✅ **كل إعلانات AdMob مدمجة**: Banner (الرئيسية)، Interstitial (بين الصفحات، كل 3 انتقالات)، Native Advanced (فالمنتدى كل 5 منشورات)، Rewarded + Rewarded Interstitial (جاهزين للاستعمال)، App Open (عند فتح التطبيق)
- ✅ **تعليقات كاملة** تحت كل منشور فالمنتدى
- ✅ **إشعارات Push** (استقبال + عرض) عبر Firebase Cloud Messaging
- ✅ **لوحة تحكم ويب** (`admin-panel/`) باش تزيد مواد وPDFs عن بعد بلا ما تدخل لـ Firestore Console يدويا
- ✅ قواعد أمان مقترحة لـ Firestore و Storage (`firestore.rules`, `storage.rules`)

---

## ⚙️ الخطوات اللي خاصك ديرها قبل ما يخدم

### 1. إنشاء مشروع Firebase
1. روح لـ https://console.firebase.google.com وزيد مشروع جديد
2. زيد تطبيق Android بـ package name: `com.etudiantsdroitmaroc.app`
3. حمل ملف `google-services.json` وحطو فـ `app/google-services.json` (محليا فقط، ماشي فـ GitHub)

### 2. تفعيل Google Sign-In
1. فـ Firebase Console > Authentication > Sign-in method > فعّل **Google**
2. زيد الـ **SHA-1** ديال التطبيق (من Android Studio أو `keytool`) فـ إعدادات المشروع بـ Firebase

### 3. Firestore - هيكلة البيانات
خاصك تزيد المواد يدويا (أو بأداة إدارة) بهاد الشكل:

```
subjects/{subjectId}
  name: "القانون المدني"
  category: "private"   // أو "public"
  orderIndex: 1

subjects/{subjectId}/pdfs/{pdfId}
  title: "محاضرات في قانون الأسرة"
  storageUrl: "https://firebasestorage.../file.pdf"
  fileSizeKb: 850
```

### 4. تخزين ملفات PDF (بلا Firebase Storage)

⚠️ Firebase Storage دابا كيطلب ترقية لـ Blaze plan (يحتاج بطاقة بنكية). كبديل مجاني 100%، كنستعملو **GitHub** لتخزين ملفات PDF:

1. زيد فولدر `pdfs/` فـ أي repo GitHub ديالك (يقدر يكون نفس repo ديال التطبيق)
2. ارفع ملفات PDF ليه
3. افتح كل ملف واضغط زر **"Raw"** - خذ الرابط من شريط العنوان (كيبدا بـ `raw.githubusercontent.com`)
4. الصق هاد الرابط فحقل `storageUrl` (يدويا فـ Firestore، أو عبر لوحة التحكم `admin-panel/`)

التطبيق كيقرا أي رابط HTTP مباشر، بلا ما يهمو منين جا.

إلا فبعد بغيتي تستعمل Firebase Storage الحقيقي (بعد ما تكون عندك بطاقة وترقي لـ Blaze)، التطبيق غادي يخدم بلا تغيير فالكود.

### 5. GitHub Actions - إعداد Secret
1. حول `google-services.json` لـ base64:
   ```
   base64 -i app/google-services.json | pbcopy   # أو تستعمل أي أداة base64 أونلاين
   ```
2. فـ GitHub repo: Settings > Secrets and variables > Actions > New repository secret
   - الاسم: `GOOGLE_SERVICES_JSON`
   - القيمة: النص المشفر (base64)
3. ادفع (`push`) للـ `main` branch، وغادي يبدأ البناء أوتوماتيكيا

### 6. لوحة التحكم (Admin Panel) - زيادة PDFs عن بعد

مجلد `admin-panel/` فيه صفحة ويب بسيطة كتخليك تزيد مواد وPDFs بلا ما تدخل لـ Firestore Console:

1. فتح `admin-panel/firebase-config.js` وبدل القيم بإعدادات مشروع Firebase ديالك (من Project Settings > زيد تطبيق ويب "</> Add app")
2. حل `admin-panel/index.html` فالمتصفح (أو استضفه فـ Firebase Hosting المجاني)
3. سجل الدخول بحساب Google، وغادي تبان ليك الـ UID ديالك فرسالة تنبيه
4. دور UID فمكان وحد:
   - `admin-panel/index.html` → `ADMIN_UID`
   - `firestore.rules` → كل `REPLACE_WITH_YOUR_UID`
5. طبق القواعد ديال `firestore.rules` فـ Firebase Console (Firestore > Rules)
   (`storage.rules` ماشي محتاجينها دابا حيت كنستعملو GitHub بدل Firebase Storage)
6. دابا تقدر تزيد مواد، ترفع PDFs مباشرة، وتحذفهم - وغادي يبانو مباشرة فالتطبيق (بلا تحديث/بناء APK جديد)

### 7. إشعارات Push
- التطبيق كيستقبل ويعرض الإشعارات أوتوماتيكيا (`NotificationService.kt`)
- باش تبعت إشعار لكل الطلبة: Firebase Console > Cloud Messaging > "Send test message" أو "New campaign"، واختار الـ Topic: `all_students`
- إلا بغيتي إشعارات أوتوماتيكية (مثلا: عند نشر منشور جديد)، تحتاج **Cloud Function** بسيطة تسمعها Firestore وتبعت push - نقدر نبنيها ليك فمرحلة جاية

---

## 📂 أقسام المشروع

```
app/src/main/java/com/etudiantsdroitmaroc/app/
  ├── ui/auth/          → تسجيل الدخول
  ├── ui/home/          → الرئيسية + المواد
  ├── ui/subject/       → لائحة PDFs لكل مادة
  ├── ui/pdf/           → قارئ PDF (أوفلاين/أونلاين)
  ├── ui/forum/         → المنتدى
  ├── ui/chat/          → الدردشة المباشرة
  ├── ui/profile/       → البروفايل
  ├── data/model/       → نماذج البيانات
  ├── data/local/       → Room (كاش PDFs أوفلاين)
  └── data/remote/      → Firebase repositories
```

---

## 🔜 المرحلة الجاية (اقتراحات)
- إضافة Interstitial Ads بين الصفحات
- صفحة تعليقات كاملة تحت كل منشور فالمنتدى
- إشعارات Push (Firebase Messaging) عند وصول رسالة جديدة
- لوحة تحكم Admin بسيطة (Web) باش تزيد المواد وPDFs بلا ما تدخل يدويا لـ Firestore Console
