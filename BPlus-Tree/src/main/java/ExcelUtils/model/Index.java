package ExcelUtils.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 索引类
 */
@Data
public class Index {
    @ExcelProperty("indexName")
    Integer key;

    @ExcelProperty("indexNum")
    String value;

    @ExcelProperty("height")
    Integer height;
}
