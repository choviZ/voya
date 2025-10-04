package com.zcw.voya.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户额度枚举
 *
 * @author zcw
 */
@Getter
public enum UserQuotaTypeEnum {

    CREATE_APP("作品数量","createAppLimit"),
    CHAT_APP("对话次数","chatLimit");

    private final String text;

    private final String value;

    UserQuotaTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserQuotaTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserQuotaTypeEnum anEnum : UserQuotaTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
