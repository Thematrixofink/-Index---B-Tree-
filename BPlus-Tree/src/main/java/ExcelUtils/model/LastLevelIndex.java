package ExcelUtils.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class LastLevelIndex {

    @ExcelProperty("indexName")
    Integer key;

    @ExcelProperty("entryPageNum")
    String value;

    @ExcelProperty("isInner")
    Integer height;

}
