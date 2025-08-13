package com.zcw.voya.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;

import java.io.File;


/**
 * 保存代码文件的模板
 */
public abstract class CodeFileSaverTemplate<T> {

    /**
     * 文件保存根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 模板方法
     * @param result
     * @return
     */
    public final File saveCode(T result) {
        // 1. 校验
        validateInput(result);
        // 2. 构建唯一目录
        String path = buildUniqueDir();
        // 3. 保存代码文件
        saveCodeFiles(result, path);
        // 4. 返回文件目录对象
        return new File(path);
    }

    /**
     * 校验输入参数 （可由子类覆盖）
     * @param result
     */
    protected void validateInput(T result) {
        ThrowUtils.throwIf(result == null, ErrorCode.PARAMS_ERROR,"代码生产结果对象为空");
    }

    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     */
    protected final String buildUniqueDir() {
        CodeGenTypeEnum codeGenType = getCodeGenType();
        String uniqueDirName = StrUtil.format("{}_{}", codeGenType.getValue(), IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件的工具方法
     * @param dirPath 文件路径
     * @param fileName 文件名
     * @param content 文件内容
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {
        File file = new File(dirPath + File.separator + fileName);
        FileUtil.writeUtf8String(content, file);
    }

    /**
     * 获取代码生成类型 （子类实现）
     * @return
     */
    protected abstract CodeGenTypeEnum getCodeGenType();

    /**
     * 保存代码文件 （子类实现）
     * @param result
     * @param dirPath
     * @return
     */
    protected abstract void saveCodeFiles(T result, String dirPath);
}
