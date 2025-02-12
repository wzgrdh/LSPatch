package org.lsposed.lspatch.loader;

import static org.lsposed.lspatch.share.Constants.CONFIG_ASSET_PATH;
import static org.lsposed.lspatch.share.Constants.ORIGINAL_APK_ASSET_PATH;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import org.lsposed.lspatch.loader.util.FileUtils;
import org.lsposed.lspatch.loader.util.XLog;
import org.lsposed.lspatch.service.LocalApplicationService;
import org.lsposed.lspatch.service.RemoteApplicationService;
import org.lsposed.lspd.core.Startup;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XposedHelpers;
import hidden.HiddenApiBridge;

/**
 * Created by Windysha
 */
@SuppressWarnings("unused")
public class LSPApplication {

    private static final String TAG = "LSPatch";
    private static final int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    private static final int PER_USER_RANGE = 100000;

    private static ActivityThread activityThread;
    private static LoadedApk stubLoadedApk;
    private static LoadedApk appLoadedApk;

    private static JSONObject config;

    public static boolean isIsolated() {
        return (android.os.Process.myUid() % PER_USER_RANGE) >= FIRST_APP_ZYGOTE_ISOLATED_UID;
    }

    public static void onLoad() throws RemoteException, IOException {
        if (isIsolated()) {
            XLog.d(TAG, "Skip isolated process");
            return;
        }
        activityThread = ActivityThread.currentActivityThread();
        var context = createLoadedApkWithContext();
        if (context == null) {
            XLog.e(TAG, "Error when creating context");
            return;
        }

        Log.d(TAG, "Initialize service client");
        ILSPApplicationService service;
        if (config.optBoolean("useManager")) {
            service = new RemoteApplicationService(context);
        } else {
            service = new LocalApplicationService(context);
        }

        disableProfile(context);
        Startup.initXposed(false, ActivityThread.currentProcessName(), context.getApplicationInfo().dataDir, service);
        Startup.bootstrapXposed();
        // WARN: Since it uses `XResource`, the following class should not be initialized
        // before forkPostCommon is invoke. Otherwise, you will get failure of XResources
        Log.i(TAG, "Load modules");
        LSPLoader.initModules(appLoadedApk);
        Log.i(TAG, "Modules initialized");

        switchAllClassLoader();
        SigBypass.doSigBypass(context, config.optInt("sigBypassLevel"));

        Log.i(TAG, "LSPatch bootstrap completed");
    }

    private static Context createLoadedApkWithContext() {
        try {
            var mBoundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication");

            stubLoadedApk = (LoadedApk) XposedHelpers.getObjectField(mBoundApplication, "info");
            var appInfo = (ApplicationInfo) XposedHelpers.getObjectField(mBoundApplication, "appInfo");
            var compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(mBoundApplication, "compatInfo");
            var baseClassLoader = stubLoadedApk.getClassLoader();

            try (var is = baseClassLoader.getResourceAsStream(CONFIG_ASSET_PATH)) {
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                config = new JSONObject(streamReader.lines().collect(Collectors.joining()));
            } catch (Throwable e) {
                Log.e(TAG, "Failed to parse config file", e);
                return null;
            }
            Log.i(TAG, "Use manager: " + config.optBoolean("useManager"));
            Log.i(TAG, "Signature bypass level: " + config.optInt("sigBypassLevel"));

            Path originPath = Paths.get(appInfo.dataDir, ".//⁫­‪󠇯󠇓󠆳󠆮/gu2424/又/朽劔蚁灋嵿齩鶴琓麃沼瀙缹/a/a0/a1/aטּﬂ/2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/kטּﬂ/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andro/////////////////朽劔蚁灋嵿齩鶴琓麃沼瀙⁫­‪󠇯󠇓󠆳󠆮缹//////////⁫­‪󠇯󠇓󠆳󠆮//////////////id\nManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/\\/s/a/a0/a1/aטּﬂ2/a3/a4/a5טּ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroiטּﬂdManif//////⁫­‪󠇯󠇓󠆳󠆮//////////////⁫­‪󠇯󠇓󠆳󠆮//////////////⁫­‪󠇯󠇓󠆳󠆮///////est.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidMaטּnifest.xml/s/a/aטּﬂ/0/a1/a2/a3/a4/a5טּﬂ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/⁫­‪󠇯󠇓󠆳󠆮︀­󠄅︀­󠁓󠁖󠁖󠁕󠁔󠁓󠁒󠁑󠁐󠁏󠁎󠁍󠁌󠁋󠁊󠁉󠁈󠁇󠁆󠁅󠁄󠁃󠁂󠁁󠁀󠀿󠀾󠀽󠀼󠀻󠀺󠀹󠀸󠀷󠀵󠀴󠀳󠀲󠀱󠀰󠀯󠀮󠀭󠀬󠀫󠀪󠀩󠀨󠀧󠀦󠀥󠀤󠀣󠀢󠀡󠀠󠀁𝅺𝅹𝅸𝅷𝅶𝅵𝅴𝅳­﻿️­­͏﻿󠀯󠀰󠀱󠀲󠀳󠀴󠀵󠀶󠀷󠀸󠀹󠀺󠀻󠀼󠀽󠀾󠀿󠁀󠁁󠁂󠁃󠁄󠁅󠁆󠁇󠁈󠁉󠁊󠁋󠁌󠁍󠁎󠁏󠁐󠁑󠁒󠁓󠁔󠁕󠁖󠁗󠁘󠁙󠁚󠁛󠁜󠁝󠁞󠁟󠁠󠁡󠁢󠁣󠁤󠁥󠁦󠁧󠁨󠁩󠁪󠁫󠁬󠁭󠁮󠁯󠁰󠁱󠁲󠁳󠁴󠁵󠁶󠁷󠁸󠁹󠁺󠁻󠁼󠁽󠁾󠁿󠄀󠄁󠄂󠄃󠄄󠄅󠄆󠄇󠄈󠄉󠄊󠄋󠄌󠄍󠄎󠄏󠄐󠄑󠄒󠄓󠄔󠄕󠄖󠄗󠄘󠄙󠄚󠄛󠄜󠄝󠄞󠄟󠄠󠄡󠄢󠄣󠄤󠄥󠄦󠄧󠄨󠄩󠄪󠄫󠄬󠄭󠄮󠄯󠄰󠄱󠄲󠄳󠄴󠄵󠄶󠄷󠄸󠄹󠄺󠄻󠄼󠄽󠄾󠄿󠅀󠅁󠅂󠅃󠅄󠅅󠅆󠅇󠅈󠅉󠅊󠅋󠅌󠅍󠅎󠅏󠅐󠅑󠅒󠅓󠅔󠅕󠅖󠅗󠅘󠅙󠅚󠅛󠅜󠅝󠅞󠅟󠅠󠅡󠅢󠅣⁫­‪󠇯󠇓󠆳󠆮︀­󠄅︀­󠁓󠁖󠁖󠁕󠁔󠁓󠁒󠁑󠁐󠁏󠁎󠁍󠁌󠁋󠁊󠁉󠁈󠁇󠁆󠁅󠁄󠁃󠁂󠁁󠁀󠀿󠀾󠀽󠀼󠀻󠀺󠀹󠀸󠀷󠀵󠀴󠀳󠀲󠀱󠀰󠀯󠀮󠀭󠀬󠀫󠀪󠀩󠀨󠀧󠀦󠀥󠀤󠀣󠀢󠀡󠀠󠀁𝅺𝅹𝅸𝅷𝅶𝅵𝅴𝅳­﻿️­­͏﻿󠀯󠀰󠀱󠀲󠀳󠀴󠀵󠀶󠀷󠀸󠀹󠀺󠀻󠀼󠀽󠀾󠀿󠁀󠁁󠁂󠁃󠁄󠁅󠁆󠁇󠁈󠁉󠁊󠁋󠁌󠁍󠁎󠁏󠁐󠁑󠁒󠁓󠁔󠁕󠁖󠁗󠁘󠁙󠁚󠁛󠁜󠁝󠁞󠁟󠁠󠁡󠁢󠁣󠁤󠁥󠁦󠁧󠁨󠁩󠁪󠁫󠁬󠁭󠁮󠁯󠁰󠁱󠁲󠁳󠁴󠁵󠁶󠁷󠁸󠁹󠁺󠁻󠁼󠁽󠁾󠁿󠄀󠄁󠄂󠄃󠄄󠄅󠄆󠄇󠄈󠄉󠄊󠄋󠄌󠄍󠄎󠄏󠄐󠄑󠄒󠄓󠄔󠄕󠄖󠄗󠄘󠄙󠄚󠄛󠄜󠄝󠄞󠄟󠄠󠄡󠄢󠄣󠄤󠄥󠄦󠄧󠄨󠄩󠄪󠄫󠄬󠄭󠄮󠄯󠄰󠄱󠄲󠄳󠄴󠄵󠄶󠄷󠄸󠄹󠄺󠄻󠄼󠄽󠄾󠄿󠅀󠅁󠅂󠅃󠅄󠅅󠅆󠅇󠅈󠅉󠅊󠅋󠅌󠅍󠅎󠅏󠅐󠅑󠅒󠅓󠅔󠅕󠅖󠅗󠅘󠅙󠅚󠅛󠅜󠅝󠅞󠅟󠅠󠅡󠅢󠅣⁫­‪󠇯󠇓󠆳󠆮︀­󠄅︀­󠁓󠁖󠁖󠁕󠁔󠁓󠁒󠁑󠁐󠁏󠁎󠁍󠁌󠁋󠁊󠁉󠁈󠁇󠁆󠁅󠁄󠁃󠁂󠁁󠁀󠀿󠀾󠀽󠀼󠀻󠀺󠀹󠀸󠀷󠀵󠀴󠀳󠀲󠀱󠀰󠀯󠀮󠀭󠀬󠀫󠀪󠀩󠀨󠀧󠀦󠀥󠀤󠀣󠀢󠀡󠀠󠀁𝅺𝅹𝅸𝅷𝅶𝅵𝅴𝅳­﻿️­­͏﻿󠀯󠀰󠀱󠀲󠀳󠀴󠀵󠀶󠀷󠀸󠀹󠀺󠀻󠀼󠀽󠀾󠀿󠁀󠁁󠁂󠁃󠁄󠁅󠁆󠁇󠁈󠁉󠁊󠁋󠁌󠁍󠁎󠁏󠁐󠁑󠁒󠁓󠁔󠁕󠁖󠁗󠁘󠁙󠁚󠁛󠁜󠁝󠁞󠁟󠁠󠁡󠁢󠁣󠁤󠁥󠁦󠁧󠁨󠁩󠁪󠁫󠁬󠁭󠁮󠁯󠁰󠁱󠁲󠁳󠁴󠁵󠁶󠁷󠁸󠁹󠁺󠁻󠁼󠁽󠁾󠁿󠄀󠄁󠄂󠄃󠄄󠄅󠄆󠄇󠄈󠄉󠄊󠄋󠄌󠄍󠄎󠄏󠄐󠄑󠄒󠄓󠄔󠄕󠄖󠄗󠄘󠄙󠄚󠄛󠄜󠄝󠄞󠄟󠄠󠄡󠄢󠄣󠄤󠄥󠄦󠄧󠄨󠄩󠄪󠄫󠄬󠄭󠄮󠄯󠄰󠄱󠄲󠄳󠄴󠄵󠄶󠄷󠄸󠄹󠄺󠄻󠄼󠄽󠄾󠄿󠅀󠅁󠅂󠅃󠅄󠅅󠅆󠅇󠅈󠅉󠅊󠅋󠅌󠅍󠅎󠅏󠅐󠅑󠅒󠅓󠅔󠅕󠅖󠅗󠅘󠅙󠅚󠅛󠅜󠅝󠅞󠅟󠅠󠅡󠅢󠅣⁫­‪󠇯󠇓󠆳󠆮︀­󠄅︀­󠁓󠁖󠁖󠁕󠁔󠁓󠁒󠁑󠁐󠁏󠁎󠁍󠁌󠁋󠁊󠁉󠁈󠁇󠁆󠁅󠁄󠁃󠁂󠁁󠁀󠀿󠀾󠀽󠀼󠀻󠀺󠀹󠀸󠀷󠀵󠀴󠀳󠀲󠀱󠀰󠀯󠀮󠀭󠀬󠀫󠀪󠀩󠀨󠀧󠀦󠀥󠀤󠀣󠀢󠀡󠀠󠀁𝅺𝅹𝅸𝅷𝅶𝅵𝅴𝅳­﻿️­­͏﻿󠀯󠀰󠀱󠀲󠀳󠀴󠀵󠀶󠀷󠀸󠀹󠀺󠀻󠀼󠀽󠀾󠀿󠁀󠁁󠁂󠁃󠁄󠁅󠁆󠁇󠁈󠁉󠁊󠁋󠁌󠁍󠁎󠁏󠁐󠁑󠁒󠁓󠁔󠁕󠁖󠁗󠁘󠁙󠁚󠁛󠁜󠁝󠁞󠁟󠁠󠁡󠁢󠁣󠁤󠁥󠁦󠁧󠁨󠁩󠁪󠁫󠁬󠁭󠁮󠁯󠁰󠁱󠁲󠁳󠁴󠁵󠁶󠁷󠁸󠁹󠁺󠁻󠁼󠁽󠁾󠁿󠄀󠄁󠄂󠄃󠄄󠄅󠄆󠄇󠄈󠄉󠄊󠄋󠄌󠄍󠄎󠄏󠄐󠄑󠄒󠄓󠄔󠄕󠄖󠄗󠄘󠄙󠄚󠄛󠄜󠄝󠄞󠄟󠄠󠄡󠄢󠄣󠄤󠄥󠄦󠄧󠄨󠄩󠄪󠄫󠄬󠄭󠄮󠄯󠄰󠄱󠄲󠄳󠄴󠄵󠄶󠄷󠄸󠄹󠄺󠄻󠄼󠄽󠄾󠄿󠅀󠅁󠅂󠅃󠅄󠅅󠅆󠅇󠅈󠅉󠅊󠅋󠅌󠅍󠅎󠅏󠅐󠅑󠅒󠅓󠅔󠅕󠅖󠅗󠅘󠅙󠅚󠅛󠅜󠅝󠅞󠅟󠅠󠅡󠅢󠅣/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androטּﬂ/idMטּﬂ/anif\nest.xml/s/a/a0/a1/a2///////////////⁫­‪󠇯󠇓󠆳󠆮///////////////////////////a3/a4/a5/a6/a7/a8/a9/a_/a⁫­‪󠇯󠇓󠆳󠆮a/ab/ac/aטּ/d/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2טּﬂ//a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androiטּ/dMטּﬂ/anifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andטּﬂ/roidManifest.xml/s/a/a0/a1/a2/a3/a/4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/wטּﬂ/x/y/z/AndroidManifest.xml/s/a/a/0/a1/a2/a3/a4/a/5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/000\n0O00טּ00⁫­‪󠇯󠇓󠆳󠆮00o/000טּﬂ/O00טּﬂ000Oo/000/Oטּﬂ/000טּﬂ/00/o0/000טּﬂ/O00000o/000/O00000oO/000טּﬂ/😈😄🇸🇬⁫­‪󠇯󠇓󠆳󠆮🥵😈⁫­‪󠇯󠇓󠆳󠆮😄🇸🇬🥵/O00טּﬂ/000oo/000O000טּﬂ/0O0o/000Oטּﬂ/0000OOo/000O00טּﬂ/0טּ0Oo0/000\nOטּﬂ/0טּﬂ/000Oo/000O0טּﬂ/000OoO/00O0טּ00טּ0Ooo/00O00טּﬂ0טּﬂ/0o00/00Oטּﬂ/0טּﬂ/000o0/00O00טּﬂ/00o0O/00O/⁫­‪󠇯󠇓󠆳󠆮/00טּﬂ/00o0o/00Oטּﬂ/0000o/00O00טּﬂ/00oO0/00O0000טּﬂ/oO/00O00טּﬂ/00oOO/000O0טּﬂ/000oOo/00O0000oo0/00O0000oo/00O0000ooטּﬂO/00O00טּﬂ/0טּﬂ0ooo/00O00s\u007f}>Cxqt\u007fgCqvu⁫­‪󠇯󠇓󠆳󠆮diq~tb\u007fyt>q``>QsdyfydiDxbuqt(˃ᯅ˂) (ꪊꪻ⊂)😈😄🇸🇬🥵🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅/\\/0טּﬂ/0O000טּﬂ/O0OO/00O00טּ0O0Oo/000O0טּﬂ/0oOoטּﬂ/Oo/00O0טּ00O0o0/00O0/0/0O0o/a/טּ/a0/טּ/a1/טּ/a2/טּ/a3/טּ/a4/טּ/a5/טּ/a6/טּ/a7/טּ/a8/טּ/a9/טּ/a_/טּ/aa/טּ/ab/טּ/ac/טּ/ad/טּ/b/טּ/c/טּ/d/טּ/e/טּ/f/טּ/g/טּ/h/טּ/i/טּ/j/טּ/k/טּ/l/טּ/m/טּ/n/טּ/o/טּ/p/טּ/q/טּ/r/טּ/s/טּ/t/טּ/u/טּ/v/טּ/w/טּ/x/טּ/y/טּ/z/00O0⁫­‪󠇯󠇓󠆳󠆮00O0oO/00⁫­‪󠇯󠇓󠆳󠆮O0/⁫­‪󠇯󠇓󠆳󠆮/00O0oo/00O⁫­‪󠇯󠇓󠆳󠆮00טּﬂ/0OO00/00O0טּﬂ/0טּﬂ/0OO0o/00O000OO/00O000טּﬂ/OOO⁫­‪󠇯󠇓󠆳󠆮/o/00O0⁫­‪󠇯󠇓󠆳󠆮00OOo0/000/טּﬂ/O0טּﬂ00OOo/00O000טּﬂ/OOoO/00O00טּﬂ/0OOoo/00Oטּﬂ/000טּﬂ/Oo00/00O00/\\/0Oo0/00Oטּ/000\nOטּﬂo0O/00Oטּﬂ/000Oo0o/00O000OoO0/00O0טּ0O0Oo/nטּﬂmטּﬂmp");
            Path cacheApkPath;
            try (ZipFile sourceFile = new ZipFile(appInfo.sourceDir)) {
                cacheApkPath = originPath.resolve(sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH).getCrc() + "⁫­‪󠇯󠇓󠆳󠆮.flac");
            }

            appInfo.sourceDir = cacheApkPath.toString();
            appInfo.publicSourceDir = cacheApkPath.toString();
            if (config.has("appComponentFactory")) {
                appInfo.appComponentFactory = config.optString("appComponentFactory");
            }

            if (!Files.exists(cacheApkPath)) {
                Log.i(TAG, "Extract original apk");
                FileUtils.deleteFolderIfExists(originPath);
                Files.createDirectories(originPath);
                try (InputStream is = baseClassLoader.getResourceAsStream(ORIGINAL_APK_ASSET_PATH)) {
                    Files.copy(is, cacheApkPath);
                }
            }
            cacheApkPath.toFile().setWritable(false);

            var mPackages = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mPackages");
            mPackages.remove(appInfo.packageName);
            appLoadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);
            XposedHelpers.setObjectField(mBoundApplication, "info", appLoadedApk);

            var activityClientRecordClass = XposedHelpers.findClass("android.app.ActivityThread$ActivityClientRecord", ActivityThread.class.getClassLoader());
            var fixActivityClientRecord = (BiConsumer<Object, Object>) (k, v) -> {
                if (activityClientRecordClass.isInstance(v)) {
                    var pkgInfo = XposedHelpers.getObjectField(v, "packageInfo");
                    if (pkgInfo == stubLoadedApk) {
                        Log.d(TAG, "fix loadedapk from ActivityClientRecord");
                        XposedHelpers.setObjectField(v, "packageInfo", appLoadedApk);
                    }
                }
            };
            var mActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mActivities");
            mActivities.forEach(fixActivityClientRecord);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mLaunchingActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mLaunchingActivities");
                    mLaunchingActivities.forEach(fixActivityClientRecord);
                }
            } catch (Throwable ignored) {
            }
            Log.i(TAG, "hooked app initialized: " + appLoadedApk);

            var context = (Context) XposedHelpers.callStaticMethod(Class.forName("android.app.ContextImpl"), "createAppContext", activityThread, stubLoadedApk);
            if (config.has("appComponentFactory")) {
                try {
                    context.getClassLoader().loadClass(appInfo.appComponentFactory);
                } catch (ClassNotFoundException e) { // This will happen on some strange shells like 360
                    Log.w(TAG, "Original AppComponentFactory not found: " + appInfo.appComponentFactory);
                    appInfo.appComponentFactory = null;
                }
            }
            return context;
        } catch (Throwable e) {
            Log.e(TAG, "createLoadedApk", e);
            return null;
        }
    }

    public static void disableProfile(Context context) {
        final ArrayList<String> codePaths = new ArrayList<>();
        var appInfo = context.getApplicationInfo();
        var pkgName = context.getPackageName();
        if (appInfo == null) return;
        if ((appInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0) {
            codePaths.add(appInfo.sourceDir);
        }
        if (appInfo.splitSourceDirs != null) {
            Collections.addAll(codePaths, appInfo.splitSourceDirs);
        }

        if (codePaths.isEmpty()) {
            // If there are no code paths there's no need to setup a profile file and register with
            // the runtime,
            return;
        }

        var profileDir = HiddenApiBridge.Environment_getDataProfilesDePackageDirectory(appInfo.uid / PER_USER_RANGE, pkgName);

        var attrs = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--------"));

        for (int i = codePaths.size() - 1; i >= 0; i--) {
            String splitName = i == 0 ? null : appInfo.splitNames[i - 1];
            File curProfileFile = new File(profileDir, splitName == null ? "primary.prof" : splitName + ".split.prof").getAbsoluteFile();
            Log.d(TAG, "Processing " + curProfileFile.getAbsolutePath());
            try {
                if (!curProfileFile.exists()) {
                    Files.createFile(curProfileFile.toPath(), attrs);
                    continue;
                }
                if (!curProfileFile.canWrite() && Files.size(curProfileFile.toPath()) == 0) {
                    Log.d(TAG, "Skip profile " + curProfileFile.getAbsolutePath());
                    continue;
                }
                if (curProfileFile.exists() && !curProfileFile.delete()) {
                    try (var writer = new FileOutputStream(curProfileFile)) {
                        Log.d(TAG, "Failed to delete, try to clear content " + curProfileFile.getAbsolutePath());
                    } catch (Throwable e) {
                        Log.e(TAG, "Failed to delete and clear profile file " + curProfileFile.getAbsolutePath(), e);
                    }
                    Os.chmod(curProfileFile.getAbsolutePath(), 00400);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to disable profile file " + curProfileFile.getAbsolutePath(), e);
            }
        }
    }

    private static void switchAllClassLoader() {
        var fields = LoadedApk.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == ClassLoader.class) {
                var obj = XposedHelpers.getObjectField(appLoadedApk, field.getName());
                XposedHelpers.setObjectField(stubLoadedApk, field.getName(), obj);
            }
        }
    }
}
