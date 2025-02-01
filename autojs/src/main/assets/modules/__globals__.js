
module.exports = function (runtime, global) {
    importClass("java.text.SimpleDateFormat");
    // Added by ozobi - 2025/02/01 > 添加: 跟踪打印
    global.traceLog = function(_msg, _logToFilePath){
        let err = new Error();
        let lines = err.stack.split("\n");
        lines.shift();
        lines.pop();
        lines.reverse();
        let callInfo = "时间: " + global.dateFormat(Date.now(),"yyyy-MM-dd HH:mm:ss.SSS");
        for (let index = 0; index < lines.length; index++) {
            let info = lines[index].replace("\tat ", "");
            let lineNum = info.slice(info.lastIndexOf(":") + 1, info.length);
            let msg = `\n${"\t".repeat(index)} <行号>: ${lineNum}`;
            callInfo += msg;
        }
        callInfo += `\n<打印信息>: ${_msg}\n`;
        if (_logToFilePath) {
            try {
                _logToFilePath = runtime.files.path(_logToFilePath);
                runtime.files.createWithDirs(_logToFilePath);
                if (files.isFile(_logToFilePath)) {
                    runtime.files.append(_logToFilePath, callInfo + "\n");
                    log("输出到文件: " + _logToFilePath);
                } else {
                    log("+++创建文件失败: " + _logToFilePath);
                }
            } catch (e) {
                log("尝试输出到文件失败:" + _logToFilePath);
                log(e);
            }
        }
        log(callInfo);
        return callInfo;
    }
    // <
    // Added by ozobi - 2025/02/01 > 时间戳转换
    global.dateFormat = function(timestamp, format){
        let fm = format === undefined ? "yyyy-MM-dd HH:mm:ss.SSS" : format;
        let date = new Date(timestamp); // 创建Date对象
        let sdf = new SimpleDateFormat(fm); // 定义格式
        let formattedDate = sdf.format(date); // 格式化时间
        return formattedDate;
    }
    // <

    global.toast = function (text) {
        runtime.toast(text);
    }

    global.toastLog = function (text) {
        runtime.toast(text);
        global.log(text);
    }

    global.sleep = function (t) {
        if (ui.isUiThread()) {
            throw new Error("不能在ui线程执行阻塞操作，请使用setTimeout代替");
        }
        runtime.sleep(t);
    }

    global.isStopped = function () {
        return runtime.isStopped();
    }

    global.isShuttingDown = global.isShopped;

    global.notStopped = function () {
        return !isStopped();
    }

    global.isRunning = global.notStopped;

    global.exit = runtime.exit.bind(runtime);

    global.stop = global.exit;

    global.setClip = function (text) {
        runtime.setClip(text);
    }

    global.getClip = function (text) {
        return runtime.getClip();
    }

    global.currentPackage = function () {
        global.auto();
        return runtime.info.getLatestPackage();
    }

    global.currentActivity = function () {
        global.auto();
        return runtime.info.getLatestActivity();
    }

    global.waitForActivity = function (activity, period) {
        ensureNonUiThread();
        period = period || 200;
        while (global.currentActivity() != activity) {
            sleep(period);
        }
    }

    global.waitForPackage = function (packageName, period) {
        ensureNonUiThread();
        period = period || 200;
        while (global.currentPackage() != packageName) {
            sleep(period);
        }
    }

    function ensureNonUiThread() {
        if (ui.isUiThread()) {
            throw new Error("不能在ui线程执行阻塞操作，请在子线程或子脚本执行，或者使用setInterval循环检测当前activity和package");
        }
    }

    global.random = function (min, max) {
        if (arguments.length == 0) {
            return Math.random();
        }
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    global.setScreenMetrics = runtime.setScreenMetrics.bind(runtime);

    global.requiresApi = runtime.requiresApi.bind(runtime);
    global.requiresAutojsVersion = function (version) {
        if (typeof (version) == 'number') {
            if (compare(version, app.autojs.versionCode) > 0) {
                throw new Error("需要Auto.js版本号" + version + "以上才能运行");
            }
        } else {
            if (compareVersion(version, app.autojs.versionName) > 0) {
                throw new Error("需要Auto.js版本" + version + "以上才能运行");
            }
        }
    }

    var buildTypes = {
        release: 100,
        beta: 50,
        alpha: 0
    }

    function compareVersion(v1, v2) {
        v1 = parseVersion(v1);
        v2 = parseVersion(v2);
        log(v1, v2);
        return v1.major != v2.major ? compare(v1.major, v2.major) :
            v1.minor != v2.minor ? compare(v1.minor, v2.minor) :
                v1.revision != v2.revision ? compare(v1.revision, v2.revision) :
                    v1.buildType != v2.buildType ? compare(v1.buildType, v2.buildType) :
                        compare(v1.build, v2.build);
    }

    function compare(a, b) {
        return a > b ? 1 :
            a < b ? -1 :
                0;
    }

    function parseVersion(v) {
        var m = /(\d+)\.(\d+)\.(\d+)[ ]?(Alpha|Beta)?(\d*)/.exec(v);
        if (!m) {
            throw new Error("版本格式不合法: " + v);
        }
        return {
            major: parseInt(m[1]),
            minor: parseInt(m[2]),
            revision: parseInt(m[3]),
            buildType: buildType(m[4]),
            build: m[5] ? parseInt(m[5]) : 1
        };
    }

    function buildType(str) {
        if (str == 'Alpha') {
            return buildTypes.alpha;
        }
        if (str == 'Beta') {
            return buildTypes.beta;
        }
        return buildTypes.release;
    }
    global.Buffer = require("buffer").Buffer;
    const getBytes = com.stardust.autojs.util.ArrayBufferUtil.getBytes;
    const fromBytes = com.stardust.autojs.util.ArrayBufferUtil.fromBytes;
    global.Buffer.prototype.getBytes = function () {
        return getBytes(this.buffer)
    };
    global.Buffer.fromBytes = function (byteArr) {
        const arrBuffer = new ArrayBuffer(byteArr.length);
        fromBytes(byteArr, arrBuffer);
        return global.Buffer.from(arrBuffer)
    }

    global.zips = Object.create(runtime.zips);
    global.gmlkit = Object.create(runtime.gmlkit);
    // global.paddle = Object.create(runtime.paddle);
}