# YouTube Downloader (SnapTube-like)

تطبيق أندرويد لتنزيل فيديوهات يوتيوب على الجهاز، مصمم على غرار تطبيق SnapTube، بلغة Kotlin وبواجهة عربية كاملة.

## المميزات

- البحث عن فيديوهات يوتيوب عبر الرابط أو معرف الفيديو (بدون مفتاح API)
- عرض جميع جودات الفيديو والصوت المتاحة مع الحجم والنوع
- تحميل خلفي عبر خدمة Android Foreground Service
- إيقاف واستئناف وإلغاء التحميلات
- استئناف التحميل من حيث توقف (Resume)
- تشغيل الملفات المحملة مباشرة من التطبيق
- قاعدة بيانات محلية (Room) لحالة التقدم والسجلات
- واجهة عربية مدعومة RTL

## بنية المشروع

```
YoutubeDownloader/
├── build.gradle                     # إعدادات البناء للمشروع root
├── settings.gradle
├── gradle.properties
└── app/
    ├── build.gradle                 # تبعيات التطبيق
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/youtubedownloader/
        │   ├── App.kt                         #(Application + Room DB + Repo + Manager)
        │   ├── data/
        │   │   ├── Download.kt                  # كيان قاعدة البيانات
        │   │   ├── DownloadDao.kt               # واجهة وصول البيانات
        │   │   ├── AppDatabase.kt               # قاعدة بيانات Room
        │   │   ├── VideoInfo.kt                 # نموذج الفيديو والتيارات
        │   │   └── YouTubeApi.kt                # عميل InnerTube لاستخراج المعلومات
        │   ├── domain/
        │   │   ├── DownloadRepository.kt        # مستودع البيانات
        │   │   ├── DownloadManager.kt           # مدير المهام في الخلفية
        │   │   ├── DownloadTask.kt              # مهمة التحميل الفردية (إيقاف/استئناف)
        │   │   ├── DownloadService.kt           # خدمة خلفية للإشعارات
        │   │   └── DownloadCompleteReceiver.kt  # مستقبل إكمال التحميل
        │   └── ui/
        │       ├── MainActivity.kt              # النشاط الرئيسي مع التنقل السفلي
        │       ├── SharedViewModel.kt           # مشاركة الفيديو بين المقاطع
        │       ├── HomeViewModel.kt             # منطق البحث والجلب
        │       ├── DownloadViewModel.kt         # حال التحميلات النشطة والمكتملة
        │       ├── HomeFragment.kt              # شاشة البحث
        │       ├── VideoDetailFragment.kt       # تفاصيل الفيديو وقائمة الجودات
        │       ├── DownloadsFragment.kt         # شاشة التحميلات (نشطة / مكتملة)
        │       ├── SettingsFragment.kt          # الإعدادات
        │       ├── StreamAdapter.kt             # محوّل قائمة الجودات
        │       └── DownloadAdapter.kt           # محوّل قائمة التحميلات
        └── res/
            ├── layout/         # تخطيطات الشاشات والعناصر
            ├── drawable/       # أيقونات متجهة
            ├── values/         # الألوان، الثيمات، السلاسل، الأبعاد
            ├── menu/           # قائمة التنقل السفلي
            └── xml/          # مسارات FileProvider
```

## متطلبات البناء

- Android Studio Giraffe (2022.3.1) أو أحدث
- Android SDK compileSdk 34 / targetSdk 34 / minSdk 24
- JDK 11 أو أحدث (يتطلب Gradle AGP 7.4.2)
- اتصال إنترنت لجلب التبعيات من Google Maven وMaven Central

## خطوات البناء والتشغيل

1. افتح Android Studio واختر "Open project"ukanمجلد `YoutubeDownloader`.
2. انتظر مزامنة Gradle وتحميل التبعيات.
3. وجّه التطبيق إلى جهاز أو محاكي (minSdk 24 فأعلى).
4. اضغط Run لتشغيل التطبيق.

### أذونات مطلوبة (في المانيفست)

- `INTERNET` — للاتصال بخوادم يوتيوب
- `FOREGROUND_SERVICE` و `FOREGROUND_SERVICE_DATA_SYNC` — للتحميل في الخلفية
- `POST_NOTIFICATIONS` — لعرض إشعارات التقدم
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` (حتى API 28) — للتخزينlegacy
- عند API 29+ يتم الحفظ في دليل التطبيق الخاص (`getExternalFilesDir`) دون إذن تخزين عام.

## ملاحظات تقنية هامة

1. **استخراج الروابط**: يعتمد التطبيق على نقطة نهاية YouTube InnerTube (`youtubei/v1/player`) دون مفتاح API. يوتيوب يغيّر آلية التوقيع (cipher) دورياً؛ إذا لم يتوفّر رابط مباشر (`url`) في الاستجابة، يُحفظ التشفير في `Stream.cipher` وتظهر رسالة "الرابط المباشر غير متوفر لهذه الجودة حالياً".

2. **التخزين**: تُحفظ الملفات في `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADs)` وهو دليل خاص بالتطبيق يُحذف عند إزالة التطبيق.

3. **التشغيل**: يتم opening الملفات المحملة عبر `FileProvider` باستخدام `Intent.ACTION_VIEW`، لذا يجب وجود تطبيق مشغّل للفيديو مثبت على الجهاز.

## الترخيص

نموذج تعليمي لأغراض التعلم فقط. يُحترم حقوق الملكية وشروط خدمة يوتيوب عند الاستخدام الفعلي.
