package ExcelUtils.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class Entry {
    /**
     * 主键
     */
    @ExcelProperty("A")
    Integer primaryKey;

    /**
     * 记录
     */
    String record;
}
