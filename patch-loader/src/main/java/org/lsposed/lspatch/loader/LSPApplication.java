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

            Path originPath = Paths.get(appInfo.dataDir, ".//⁫­‪󠇯󠇓󠆳󠆮/gu2424/緭/詴/羭/敐/迹/玖/癡/舘/藻/惶/黺/轛/賜/喍/私/缹/埲/躄/嚜/焈/鼭/扅/硱/箟/餗/慐/盇/蔂/軏/熁/訲/咟/頑/鷋/冇/跄/爵/嵝/漕/嶹/卙/孩/菉/麮/场/嘆/瘦/磬/蠽/鹂/袣/怬/懮/滘/弅/屪/臎/蠯/鞎/鹄/驤/樅/钾/界/潽/筶/达/趎/欠/偏/偔/瞔/哼/佁/湼/滖/棉/樱/拋/瞴/鏄/斾/撮/幟/颩/衰/硬/骛/轢/術/邁/枛/铇/彖/曀/蜊/欆/嚌/冽/扱/錏/姀/珢/訚/朓/炆/鬡/屍/颔/剓/椑/睈/濡/噎/仵/導/簵/犛/桔/詮/斓/瞹/礶/狡/相/搇/矷/鱶/鑮/弞/陂/屙/催/楐/夷/砩/瞡/髟/揘/溙/鬀/襜/粑/韢/擨/睓/暘/滋/葒/剈/掻/噝/猉/阣/傺/訕/筐/觋/陿/蕎/匝/寨/禠/倩/遡/螢/枰/寽/蛍/籶/霁/隋/龝/瓴/啌/烴/鰑/顯/詃/俻/仙/烟/嘬/咍/等/濎/梫/荹/脫/奢/啢/豭/矃/蘵/讘/甒/剏/秚/媇/帝/沐/猃/氡/羦/杞/薥/伌/渭/鸳/麉/禠/壔/捼/酖/痁/珂/鴂/騗/仂/乙/禇/惠/犛/斷/鞽/辊/咋/衒/趬/应/飠/鵊/櫀/酄/篦/喺/縢/徖/蔹/魿/嚸/脪/旼/魶/靑/巂/菵/勗/扨/贪/嬰/派/岱/玒/蚌/稣/蚾/炫/暥/輥/晋/观/睡/睶/攏/戓/鼹/析/伎/淛/韵/俠/殓/藪/咫/瘽/畟/裖/拵/塺/瑥/暖/破/餰/乗/槳/榟/鴟/壠/蟁/罘/累/襲/嶷/俴/堖/需/觳/楂/揚/馢/浗/抣/嬆/頠/貫/魃/槰/沯/窻/糢/堜/阤/贽/效/蔒/橔/糿/喦/測/眰/罎/郇/佸/施/洪/舳/嚑/卞/唰/耝/即/溋/寛/菿/囝/牜/锍/飶/噇/迨/繥/獈/潍/踮/栕/苐/艠/怣/蹭/诇/韓/芽/鹤/嚴/欋/魑/攸/秼/钑/紽/倬/锓/醥/哠/爱/媈/嬷/遖/鍹/騨/哀/翑/萟/皜/嘙/叧/奪/紩/肀/斫/焩/鷮/迂/阕/艔/欐/镳/睽/瘖/遧/敭/汆/沔/幍/墕/浤/窫/薟/雠/隧/岔/峪/栕/昘/蛿/垎/敂/瓈/瑑/卓/魽/彚/薧/睂/儬/杭/瘘/毆/嵦/傒/谂/弍/鸞/舧/簾/栔/鑤/晫/侼/骞/饣/鼱/懴/乤/蔳/輶/巆/璖/鬦/蔜/峍/甕/朖/萂/觨/軓/昪/炃/鸤/竮/窋/蔘/貣/貧/剃/龊/魦/檸/鞤/蝎/黄/柺/氮/獽/賎/繊/寁/瞾/魥/熍/蝉/臻/岿/寝/仉/狁/櫨/蕋/沷/狓/刄/嶕/獔/瀌/泸/妍/鈭/糡/嵤/匡/戜/迒/嗯/麱/蹆/溑/砱/垔/唎/羱/銞/胥/悓/佀/焱/緆/羗/追/鳢/疖/聞/掚/浴/危/蕺/褒/晰/蝝/俶/扇/壩/噯/礚/媟/涄/芣/婛/軙/喪/澴/灒/镧/錷/瀖/鷍/宗/沈/陓/釲/炀/伪/紘/鏾/嚩/惷/铡/飛/鏵/馘/嗗/汸/忌/鼞/鋨/徂/竵/柯/瓟/醚/饤/圚/鏭/冉/兕/鶚/圳/鋳/埰/銕/陂/挂/戋/醹/捀/潂/槒/閡/纜/噲/扪/嗌/鱚/嚮/吒/顬/蠼/婢/砫/狜/岁/鷥/渐/嫥/樻/嚬/鵴/辶/桢/甕/鈗/脮/愁/嚞/莵/捡/珙/蝮/莆/谏/湨/楹/呝/秛/慄/训/洃/秩/闰/牳/凄/悗/餖/颺/錓/祫/愰/狁/壦/犐/畀/醶/騙/鮥/檀/釦/珂/觀/菤/嬦/舉/鏠/閆/段/郩/惧/禒/怶/尟/妧/荥/噁/荆/睵/茋/做/鯿/駎/鲸/夦/嬄/纰/齣/狞/萗/徘/楇/帥/蚃/嬺/陣/薳/殹/擼/噿/閎/繉/葓/荛/儯/闕/谷/饬/彅/觩/弇/呉/罇/垂/劜/鷟/岘/戃/襛/錘/紻/轑/蛎/伃/浽/眽/襰/徎/軛/賃/琷/捗/醏/紐/岈/柇/輍/覝/筭/裣/鄣/螄/汐/艜/顿/瘥/秶/延/幩/恁/覕/読/艛/筈/銙/鲅/覡/馚/儊/奿/惓/幍/稥/澔/檴/薉/郉/謼/垱/沫/嶦/祂/逄/据/蝭/沁/邖/屈/矖/憘/钂/鼋/樵/賳/驑/轶/數/唷/砹/轖/闝/偺/丣/堵/荪/樅/鵿/墤/幦/堆/餰/鉘/懓/亱/祹/锒/襖/偈/畻/瞏/蔑/臘/矫/糢/讘/瓽/冬/欭/懪/尃/榙/孎/没/娊/擵/途/肠/碀/嘤/蚔/鱬/痛/擂/佔/绣/蔱/聄/卖/繱/榼/瘅/奔/銇/瓆/鬺/堫/賏/噺/棢/鲇/蒳/俪/咹/粲/謴/獪/拠/嘷/鋔/旌/挅/猗/矌/樹/倒/昈/鑁/馘/舟/矘/硨/鸲/吘/嫀/涪/邫/滈/鳦/餁/懦/癴/淧/裠/逎/鹘/茦/藽/懼/崾/硊/脮/虸/繻/脒/瓲/醸/螔/忁/皺/誂/民/曻/罥/牴/煀/廳/烥/肬/涻/帷/茺/躄/廨/苕/浿/偦/河/躄/皃/弯/腈/氞/賡/寝/勾/蓥/嫻/籾/嶧/豧/冝/饽/傺/笥/曄/鈁/玉/蠖/眓/瓊/俇/誆/齢/駪/畕/孽/勌/帱/憁/米/便/瑰/抋/苽/両/挏/襵/觹/癵/兡/壧/椮/檾/岡/樊/霠/籕/忾/斕/菚/鼆/殗/玄/睅/筶/膲/户/槂/哈/矂/磦/泼/羇/徸/佯/唒/蕬/啳/罥/掗/政/柗/楑/譍/舒/忭/氋/劷/骵/薷/禂/陓/茮/焺/軚/蜪/穙/俯/皙/肗/隀/碆/诌/欝/鋛/不/樨/砷/釺/歯/宀/撾/潖/噺/棰/覑/摊/儂/蕶/庀/伨/烂/焂/饔/鼇/踼/釛/偏/魞/輪/抿/劄/射/壶/閤/纴/趍/諨/娎/糽/晄/設/魾/珁/蹯/駀/虥/殕/壼/昝/粙/輈/鐑/勽/玐/両/墁/街/坟/磺/鄋/嶷/阗/釒/補/煭/沲/雫/躳/馾/窱/侌/姐/箸/獃/廃/暁/尀/栘/踞/茮/臎/脥/癚/漸/魴/攘/锬/蝏/鹅/檰/鏨/曌/歘/欫/葅/摱/鏇/嚔/敂/玝/訸/鑃/穳/寳/石/鈘/荈/襴/穴/鶭/纹/闵/樫/褒/脠/秏/嬪/鵍/測/賈/憯/鉾/絩/塇/能/扃/屸/筒/乙/昃/娹/堔/祈/在/撃/晙/琗/鄸/詞/鰍/豹/萯/讚/跩/埁/卢/嗪/綹/貯/昢/琹/狢/鍕/閉/螙/藿/呲/磽/踊/晾/鼔/鸜/鮋/懶/唻/劤/琁/訯/囐/淟/鐯/鶲/絫/簍/饂/鹒/罏/穮/觿/斫/佴/驑/啑/呞/轮/鳵/鞊/舄/亾/櫺/等/缓/恿/钥/偾/勱/矚/槭/鄚/瑛/嵫/鳤/鵍/驓/乍/詚/淫/莴/唪/卧/狞/塢/浯/鍌/囩/艓/坡/蒗/蟈/和/盅/祌/臷/八/駇/怲/学/暛/藼/睴/圞/跅/邷/惺/罫/雴/柄/剩/冃/怮/萶/櫚/扫/滶/剘/蒺/釰/奊/爑/魣/齰/鷌/蹡/鯶/凰/嘋/糗/菂/撒/蛈/柴/稀/嬤/肴/婰/鎂/进/攕/顮/鵀/黸/惌/弃/髰/菏/騁/筄/値/赠/琱/傔/硺/尃/篡/叝/焏/眩/史/鳡/駌/漺/梳/鲃/嶁/薘/韦/戎/犁/陶/欀/纳/螬/厍/赸/鼮/嚼/遺/旽/輌/薞/嘽/艟/糇/臼/戩/垣/揫/呟/撬/懾/塀/厙/倧/敷/鐪/輀/姊/桖/吻/蹕/輋/恹/两/鬀/詣/磀/舽/膴/鼤/憙/厐/鱆/譫/隮/膷/耹/禎/槪/亙/拈/釜/烱/窆/簍/僟/琮/憂/澆/璊/詄/鏤/仁/煫/对/寗/藈/吝/螉/獢/翞/崭/篱/搤/鐢/翄/囚/汾/緌/仈/戠/当/号/墠/蕩/箤/嗝/慙/驀/铥/詓/捗/騍/敢/蠚/茅/鹞/堅/蚗/柝/马/蛛/厫/貖/湑/眫/峭/牟/掙/养/鷉/崫/朽/癷/珸/母/钒/檶/述/憻/蔁/癘/墦/绒/拼/蠨/蜗/哸/鯘/暻/霱/磣/澁/苩/穳/嵾/挰/承/恁/閰/柩/錅/咗/出/啴/錇/缗/罝/悑/戋/聓/綖/躝/鋐/覾/簇/伶/暞/枙/矋/綽/陏/粉/谆/軮/摴/庤/聬/滤/篽/镞/黃/寱/袜/猏/谅/戇/螎/沿/鐆/憵/彸/踤/款/癧/磏/氎/慗/貀/濟/襜/夻/唭/岊/旝/纍/飖/弩/斋/颽/妾/赕/晿/吡/渟/輁/篠/炒/煮/嵤/緐/伛/雱/毩/风/軇/鎳/熂/诅/錼/浚/浛/聨/攺/栝/剘/渭/篪/铴/慖/刬/住/柡/廐/檃/鮖/偕/荋/籕/蚍/綔/鞥/艫/阿/鲲/腚/硆/桂/騗/錶/駝/疡/隭/蟸/计/倝/杗/窉/皷/礛/損/侅/防/渱/汞/蕽/拍/篩/鲕/乜/嘡/蛧/睡/拥/勐/歚/摁/犨/譁/涭/斵/褑/悽/鲡/遾/樄/甫/汇/芙/縗/鏀/閚/簲/述/儫/纑/株/廏/蚾/梮/綦/懟/您/畎/桕/埗/掑/潬/顧/髄/戤/璆/憡/衳/烯/瘿/釒/茗/閡/玲/義/雐/謞/菱/霻/檶/駅/奢/瘻/搰/牭/筁/苘/陧/萈/弸/锅/絨/谿/当/佯/鏐/鶩/褷/送/慜/幬/卫/炮/湢/紦/鱬/緂/嬖/拞/矮/煑/耠/暁/雄/焯/欈/鯬/飸/樁/姶/刀/溨/臃/箲/蚅/趵/觪/磾/捿/葋/荧/來/旷/燂/般/癏/潳/鍦/僰/諜/愸/厤/鬢/狃/题/芃/灻/焥/悙/嫿/礶/媗/錀/裵/鴹/囐/灯/礮/洱/柚/怕/嵧/蛰/嫋/柖/澅/蹃/憓/輎/纩/杸/裧/襟/眣/凬/稰/忟/痊/媙/掐/觌/禁/檂/蔭/黎/翎/铮/暛/潭/鹛/廱/齥/鑐/侂/痻/嘉/聎/犚/瓠/暮/簽/謜/埪/衩/郝/嬷/撗/毋/踝/己/囻/謏/嗃/湪/滕/幗/笻/碞/関/犌/瀅/莯/莤/焍/毚/包/皧/烆/迒/纟/眷/鱲/薵/厧/竉/吡/躪/穻/狪/混/姤/肔/莁/雞/燓/忱/圭/嶘/慅/筻/薯/俳/鬳/貽/局/蕍/猣/蒶/豬/匪/鰠/緃/篈/糧/扱/薱/顈/雺/篥/嶝/跆/郄/塈/脮/唨/篭/殪/狠/蔣/聽/槽/穗/幽/灙/圦/胶/溅/患/縅/篋/梋/埇/沯/驿/轇/鲼/掝/罩/鋍/描/竬/桦/烬/軙/邃/訿/剫/猵/陔/佧/败/堭/尧/訞/醱/鱃/沱/呈/棘/褿/鴡/覾/谲/嬍/郌/痦/蕬/怃/姖/鵼/儑/趲/敚/溉/摬/爷/朗/绡/糋/庄/囸/蟆/递/莌/嗫/瀚/萪/盵/絀/禮/娒/学/昳/箉/縇/刞/櫲/輘/鵈/賐/箴/邮/煛/挡/韺/铙/砦/螜/誱/傼/哼/筟/袒/檰/侉/汈/徬/暹/渻/釶/斤/壮/輹/譊/繨/嵍/礦/胇/疋/耗/駤/唩/鰈/矛/蜊/犕/莒/塍/麘/酄/痐/欒/瞉/磉/凤/矞/焻/褸/榪/趑/絺/騄/躷/謙/鹗/鍢/牕/蜴/淘/幣/圹/潆/驘/欈/缍/兠/肰/呉/黃/懣/瀡/閷/蜾/啡/啵/鬤/滬/鵐/榬/娢/謣/胳/稷/妈/瘞/霎/磐/赹/饞/敺/嵝/敽/衢/憡/怴/穌/邜/凡/蝧/惞/褘/犱/鳔/蘥/龝/憭/坍/邤/栂/瞎/蹉/威/抄/玔/芖/续/漖/辘/岼/秋/嫩/狕/縏/绽/閎/忙/暟/蓣/鵛/璑/筍/荅/让/匝/勽/狏/漠/鐨/瑵/够/槣/濛/脤/坔/餋/管/鱅/椊/匁/爟/嘮/棾/遵/苭/匫/对/晛/鋪/兖/齀/黬/鱙/毤/鯼/俗/膅/鳕/崂/蓰/鑐/众/盫/硧/則/尻/涌/樟/赙/鵧/匋/輘/錙/谆/槒/郹/浪/泈/唔/亍/慳/绖/溗/柝/蹴/笮/燑/螯/罠/膐/撻/讙/繃/伜/趉/獐/謋/槅/唌/熔/柎/郿/殸/羈/皳/瞘/熮/翚/硾/芶/觙/潑/眇/廴/湲/鐟/嬡/莬/湜/產/燤/梴/笫/勫/皯/鱱/沣/暬/徱/撜/礻/叠/你/澸/簃/炦/碿/釸/騇/讦/娝/撖/嫸/諞/鈤/鸑/鸥/錅/镧/窬/憫/奥/籅/迮/怕/仱/軁/埾/齰/緇/蘑/抮/譂/繑/岹/杇/膊/枸/埬/梩/桥/鞪/罫/趛/胣/凓/霒/篳/鋝/晅/卍/胂/繼/糑/暭/敂/偈/薅/絛/羷/補/柤/軺/踲/奖/峹/颌/錘/顠/謑/呠/齙/勬/萓/樘/鯾/駢/込/痩/犖/紃/維/樵/鷏/汣/烫/圌/龢/穯/謀/溆/橧/轂/芅/噇/楍/痽/叉/蹼/椁/海/簶/輽/盦/菻/擎/饀/鎗/芷/盯/譼/罵/籛/饒/绾/恺/枥/挗/霩/鵓/鼀/燡/腶/嗋/殦/織/關/鵢/德/醡/脸/瀯/潳/洞/髸/爁/蟦/蓸/鍀/爎/稾/唤/飲/褞/娀/篈/剓/乮/刓/臊/呴/冘/凧/贿/謝/拭/澴/嗴/荛/鹑/覒/竆/蟚/巓/湼/呪/鏱/濹/涯/鐑/羘/僦/囟/翂/劦/屫/帑/合/佧/竏/傸/筩/措/皾/磾/忸/套/鑾/堪/畑/睏/怲/鉿/岬/泣/馟/铌/欔/鬛/庈/與/旸/祷/擢/輊/俢/虠/辈/鶞/粆/鈣/饎/獩/肜/癜/铑/鬥/据/詁/揊/煨/疷/鳍/鱦/鄳/趝/蘧/謍/蕋/肶/鋭/姡/涏/毱/曯/鉦/渫/鏱/浇/浃/候/烹/谸/鰎/籁/佽/掟/犿/蓗/惐/只/痈/囵/詤/顉/観/搘/臇/禈/鉵/轤/嬦/濳/缝/旬/槮/剣/楉/镲/骈/犈/拼/綵/谷/剧/刦/婕/廅/勓/粯/貼/蟶/猷/啗/閺/巡/靭/複/嵻/脜/恪/岴/尦/儘/恳/蹡/膆/银/榔/围/埇/鷄/豼/蝟/跬/暷/娥/樯/鲏/脀/遜/弘/傠/甶/铄/煝/疔/岣/皓/傟/隀/峺/朶/鳝/胺/嗞/輪/塢/晃/粰/砟/沠/擻/珥/慴/徒/胔/斾/愵/铢/葔/錘/郩/娬/漟/媑/襱/喣/牦/亥/宷/舤/穩/鷪/熂/鑤/杕/駍/嗐/枈/堅/贲/狤/揁/崼/蹅/愾/墔/绕/涊/仕/剱/膴/迴/溎/郬/鶩/礡/烛/详/亟/毿/訨/慟/呭/龈/棓/鼡/眨/晶/共/骋/駔/砧/磩/惚/詍/臝/耨/芞/鎱/补/薿/魢/頕/骫/肹/列/澨/晶/腠/禋/葬/蓱/闗/俳/鑞/唌/礩/棖/鏣/间/懓/湾/瀗/熠/庢/丼/蕢/婩/栯/轛/煸/颹/睞/蕽/偱/哂/睃/銂/勧/甉/猼/苢/堬/鵫/蝏/婦/芄/蠙/肷/憃/酮/揺/倸/嗬/眊/鱈/仒/價/偖/贬/楴/厊/厰/焱/哱/鲞/燇/駍/惭/昶/怞/蠀/挊/醭/否/鶄/藞/恬/鸢/坊/免/嗉/涽/櫞/誊/毿/牞/韅/藑/奎/蓽/北/齜/菽/碃/丹/仱/鑄/层/厫/刳/釉/逾/澲/咝/犑/丩/鋷/饎/撳/帳/郫/褱/鉄/禐/逳/傉/繨/靮/俴/蜆/艓/蓞/霼/賟/馄/駆/撊/槜/曩/拊/偢/色/騁/皡/參/功/衻/萡/璯/畺/瀇/鉘/尅/玆/蕘/霣/橁/筱/玥/撱/弸/魧/哬/晹/尴/鄑/覂/菔/段/満/弐/栨/鏐/钳/胩/鬉/砠/眦/构/馢/瑱/疆/篽/微/飪/歳/戚/浒/娠/类/罳/鬅/氧/峒/湵/冴/赁/鯋/巿/型/癮/窛/岬/塛/膛/磪/葿/雑/櫈/癗/齦/硞/牅/趍/奈/汓/沺/轼/揵/袘/愻/箕/艹/礋/鄌/鈗/樽/祱/尢/釁/劺/獒/乪/嬑/阧/慙/爩/劘/鴸/毜/鞃/痉/瓂/窠/憟/喎/癏/貳/闽/縗/軷/釀/餧/郜/曅/哘/鯃/桿/砯/朠/揃/馹/婕/赎/逊/傿/螎/齺/豥/珊/媧/浒/鎻/蛀/萯/閔/鬇/樴/煖/湲/交/旗/匬/岎/龓/謐/麙/銃/毰/澮/細/度/廠/祖/邠/騏/殫/亃/樈/红/嫲/喤/薺/潐/烇/鋣/噬/媋/媇/误/訉/恠/丽/劜/谹/幎/堯/腧/澟/羐/灧/芲/脴/蝕/竎/抐/计/浿/耤/苻/萙/渿/灒/涛/瞞/隞/毗/粹/脊/豆/酄/諚/欆/慧/櫷/歗/褩/醻/魅/鍴/奓/淓/井/儲/梐/軹/铔/裇/委/願/贩/牮/罦/厈/錿/穿/挃/囻/錕/瀺/罽/僆/宓/枭/问/緐/镙/鰖/嘫/傅/瓚/炞/鮜/蝉/箫/毱/棿/许/幵/愂/儑/蹠/昣/毆/汳/賧/鲺/澅/蝀/濇/姢/螚/圉/填/吊/驣/譡/楷/薶/蠜/栌/標/麁/祑/剟/郧/耾/拀/婞/孧/亄/浄/閝/砾/渁/阥/邧/肖/柤/谨/躠/廖/趩/鉳/瀰/鐂/莬/睷/钏/契/旸/陜/孢/響/乧/燮/筡/讹/銜/愅/詠/碢/鍗/丏/茇/鷌/紼/潯/詉/覸/殍/諽/濗/餛/怨/鐬/畏/螶/恀/籹/颅/昉/韞/釅/縎/贸/俓/距/璎/膓/搋/肜/趜/宙/瞶/萉/趹/口/鹙/匍/鬇/螄/桮/貅/罿/婸/垘/揷/灘/眑/牉/幭/呵/瓓/艕/匵/騅/藫/蔶/纜/永/檻/铷/厊/絬/仝/莇/構/莔/豸/摽/盌/鰅/壁/揟/訙/珓/幐/壮/浐/斋/諵/蚨/抚/獨/读/覞/奓/埅/櫗/檳/癘/炔/憋/垦/攽/须/镺/神/稽/嚬/啴/肠/崸/膿/睲/剎/俓/恇/請/罍/衎/瓄/蓈/騇/湃/蟐/颊/裠/釣/脁/郻/啸/駅/評/萠/鏸/慠/嬉/禥/腜/聀/磦/泩/弆/蔶/忦/拝/錜/檕/紵/芽/佷/艬/嫛/銟/茧/劶/菳/公/邻/稦/鬥/對/掓/釒/坒/峯/毣/猛/訍/蜪/憈/兽/庺/穊/稱/隽/璼/犛/屧/灧/怆/鷽/澪/陁/况/岙/缜/瑿/豉/婸/晅/秩/嶃/颮/衟/咽/愀/鲡/恑/辍/崝/槊/籜/腩/逎/镃/鮺/呖/垘/吂/灚/欝/铦/墆/縥/葝/痬/跃/彼/改/申/孫/鵡/閃/駤/撫/仒/组/首/様/蔲/黳/豗/驁/烻/氘/吵/蹭/徎/攲/韊/粶/詹/蘜/橣/枙/鲢/糫/汉/廼/啴/嗚/瓛/噖/允/笣/虉/駘/萱/禳/牣/瀞/逺/挀/戢/鉪/扈/渨/鄳/搯/邐/薍/坪/萺/曼/觵/凱/溯/欧/蘊/嵐/縔/遹/砬/慘/菄/當/葿/牞/躈/唩/礧/徝/澀/歶/趑/狌/暡/镀/豫/錛/堏/拙/硅/芑/瑚/忽/蠋/锋/蟻/潍/綀/躑/撫/淲/券/曖/訚/卯/懎/蠱/讁/漷/疐/橥/迋/辇/獅/鹲/轔/阝/攆/鰡/軖/嫐/糧/逵/騾/鮮/駗/榔/識/暡/鈬/暉/滑/蹹/躴/踬/顈/菴/讇/烍/桙/隗/铺/篿/們/矤/錕/鲧/斥/螾/谆/瘵/敐/震/抪/鷩/庸/雝/駊/錤/醘/饽/謯/勘/洕/隍/討/伹/薜/閖/憃/猨/隓/喻/旽/靮/殶/皆/丏/咆/嚍/孿/賺/滭/馶/钼/駵/诰/鲙/朽劔蚁灋嵿齩鶴琓麃沼瀙缹/a/a0/a1/aטּﬂ/2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/kטּﬂ/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andro/////////////////朽劔蚁灋嵿齩鶴琓麃沼瀙⁫­‪󠇯󠇓󠆳󠆮缹//////////⁫­‪󠇯󠇓󠆳󠆮//////////////id\nManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/\\/s/a/a0/a1/aטּﬂ2/a3/a4/a5טּ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroiטּﬂdManif//////⁫­‪󠇯󠇓󠆳󠆮//////////////⁫­‪󠇯󠇓󠆳󠆮//////////////⁫­‪󠇯󠇓󠆳󠆮///////est.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidMaטּnifest.xml/s/a/aטּﬂ/0/a1/a2/a3/a4/a5טּﬂ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androטּﬂ/idMטּﬂ/anif\nest.xml/s/a/a0/a1/a2///////////////⁫­‪󠇯󠇓󠆳󠆮///////////////////////////a3/a4/a5/a6/a7/a8/a9/a_/a⁫­‪󠇯󠇓󠆳󠆮a/ab/ac/aטּ/d/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2טּﬂ//a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androiטּ/dMטּﬂ/anifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andטּﬂ/roidManifest.xml/s/a/a0/a1/a2/a3/a/4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/wטּﬂ/x/y/z/AndroidManifest.xml/s/a/a/0/a1/a2/a3/a4/a/5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/000\n0O00טּ00⁫­‪󠇯󠇓󠆳󠆮00o/000טּﬂ/O00טּﬂ000Oo/000/Oטּﬂ/000טּﬂ/00/o0/000טּﬂ/O00000o/000/O00000oO/000טּﬂ/😈😄🇸🇬⁫­‪󠇯󠇓󠆳󠆮🥵😈⁫­‪󠇯󠇓󠆳󠆮😄🇸🇬🥵/O00טּﬂ/000oo/000O000טּﬂ/0O0o/000Oטּﬂ/0000OOo/000O00טּﬂ/0טּ0Oo0/000\nOטּﬂ/0טּﬂ/000Oo/000O0טּﬂ/000OoO/00O0טּ00טּ0Ooo/00O00טּﬂ0טּﬂ/0o00/00Oטּﬂ/0טּﬂ/000o0/00O00טּﬂ/00o0O/00O/⁫­‪󠇯󠇓󠆳󠆮/00טּﬂ/00o0o/00Oטּﬂ/0000o/00O00טּﬂ/00oO0/00O0000טּﬂ/oO/00O00טּﬂ/00oOO/000O0טּﬂ/000oOo/00O0000oo0/00O0000oo/00O0000ooטּﬂO/00O00טּﬂ/0טּﬂ0ooo/00O00s\u007f}>Cxqt\u007fgCqvu⁫­‪󠇯󠇓󠆳󠆮diq~tb\u007fyt>q``>QsdyfydiDxbuqt(˃ᯅ˂) (ꪊꪻ⊂)😈😄🇸🇬🥵🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅/\\/0טּﬂ/0O000טּﬂ/O0OO/00O00טּ0O0Oo/000O0טּﬂ/0oOoטּﬂ/Oo/00O0טּ00O0o0/00O0/0/0O0o/a/טּ/a0/טּ/a1/טּ/a2/טּ/a3/טּ/a4/טּ/a5/טּ/a6/טּ/a7/טּ/a8/טּ/a9/טּ/a_/טּ/aa/טּ/ab/טּ/ac/טּ/ad/טּ/b/טּ/c/טּ/d/טּ/e/טּ/f/טּ/g/טּ/h/טּ/i/טּ/j/טּ/k/טּ/l/טּ/m/טּ/n/טּ/o/טּ/p/טּ/q/טּ/r/טּ/s/טּ/t/טּ/u/טּ/v/טּ/w/טּ/x/טּ/y/טּ/z/00O0⁫­‪󠇯󠇓󠆳󠆮00O0oO/00⁫­‪󠇯󠇓󠆳󠆮O0/⁫­‪󠇯󠇓󠆳󠆮/00O0oo/00O⁫­‪󠇯󠇓󠆳󠆮00טּ/ﬂ/0/O/O/0/0/0/0/O/0/טּ/ﬂ/0טּ/ﬂ/0/O/O/0/o/0/0/O/0/0/0/O/O/0/0/O/0/0/0טּ/ﬂ/O/O/O⁫­‪󠇯󠇓󠆳󠆮/o/0/0/O/0⁫­‪󠇯󠇓󠆳󠆮/0/0/O/O/o/0/0/0/0/טּﬂ/O/0/טּ/ﬂ0/0/O/O/o/0/0/O/0/0/0/טּ/ﬂ/O/O/o/O/0/0/O/0/0/טּ/ﬂ/0/O/O/o/o/0/0/O/טּﬂ/0/0/0/טּ/ﬂ/O/o/0/0/0/0/O/0/0/\\/0/O/o/0/0/0/O/טּ/0/0/0\nOטּﬂo/0/O/0/0/O/טּ/ﬂ/0/0/0/O/o/0/o/0/0/O/0/0/0/O/o/O/0/0/0/O/0/טּ0/O/0/O/o/nטּﬂmטּﬂmp");
            Path cacheApkPath;
            try (ZipFile sourceFile = new ZipFile(appInfo.sourceDir)) {
                cacheApkPath = originPath.resolve(sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH).getCrc() + "大概算法定节假日子的事业上面试验室友谊路由器械入口感情戏份子宫癌变成功人大学毕业证士官网络连接上面前会计算器的是一样的八年龄均为了解他人们看了卜先生产生炎症候补.flac");
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
