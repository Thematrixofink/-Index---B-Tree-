package ExcelUtils.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class IndexRead {
    @ExcelProperty(index = 0)
    Integer key;

    @ExcelProperty(index = 1)
    String value;

    @ExcelProperty(index = 2)
    Integer height;
}
