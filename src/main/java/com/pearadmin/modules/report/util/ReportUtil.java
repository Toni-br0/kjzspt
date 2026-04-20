package com.pearadmin.modules.report.util;

public class ReportUtil {

    public static String convertToLowercaseWithUnderscore(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                // 如果不是第一个字符，在大写字母前添加下划线
                if (i > 0) {
                    result.append('_');
                }
                // 将大写字母转换为小写
                result.append(Character.toLowerCase(c));
            } else {
                // 直接添加小写字母或其他字符
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 地址排序字段内容
     * @return
     */
    public static String getSortLatnName(){
        String sortLatnName = "'乌鲁木齐','克拉玛依','吐鲁番','哈密','昌吉','博州','巴州','阿克苏','克州','喀什','和田','奎屯','伊犁','塔城','阿勒泰','石河子'"
                            + ",'长途传输局','无线网络优化中心','大数据与AI中心','网络监控维护中心','中电信数智科技有限公司','云中台','客户经营中心','全渠道运营中心','数字生活事业部'";
        return sortLatnName;
    }


}
