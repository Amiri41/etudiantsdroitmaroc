# etudiants-notify (Cloudflare Worker)

الملف `etudiants-notify.js` هو الكود اللي كيتحط فـ Cloudflare (Workers & Pages > etudiants-notify > Edit code).

## المتغيرات (Settings > Variables and Secrets)
- `FIREBASE_SERVICE_ACCOUNT` (Secret): JSON ديال service account (كيبقى كيف هو).
- `APP_SECRET` (Secret): الكلمة السرية القديمة، للنسخ القديمة من التطبيق فقط (مؤقتا).
- `LEGACY_OFF` (اختياري): `true` باش تقفل الكلمة القديمة نهائيا.
- `ADMIN_UID` (اختياري): معرّف الأدمين، وإلا ما تحطش كنستعملو الثابت اللي فالكود.

## كيفاش كيخدم
- التطبيق ولوحة الأدمن كيصيفطو `Authorization: Bearer <Firebase ID token>`.
- `kind: "chat"`: كيتأكد بلي المرسل طرف فالمحادثة، وكيجيب توكن الطرف الآخر.
- `kind: "friend_request"`: كيتأكد بلي طلب الصداقة كاين.
- `topic`: للأدمين فقط.
- القراءة من Firestore كتتدار بصلاحيات المستعمل نفسو، وقواعد Firestore هي اللي كتحكم.
