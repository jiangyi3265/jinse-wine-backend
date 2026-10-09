package com.ruoyi.system.domain.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;

/** 小程序与运营后台共用的商品目录。金额均以分保存和返回。 */
@JsonIgnoreProperties({ "productId", "sortNum", "status", "createBy", "createTime", "updateBy", "updateTime", "remark", "params" })
public class BizProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String id;
    private String name;
    private String category;
    private String spec;
    private String series;
    private String description;
    private String image;
    private Integer price;
    private Integer originalPrice;
    private Integer stock;
    private Integer sales;
    private Integer sortNum;
    private String status;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSceneId() { return productId == null ? "" : String.valueOf(productId); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Integer originalPrice) { this.originalPrice = originalPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public Integer getSortNum() { return sortNum; }
    public void setSortNum(Integer sortNum) { this.sortNum = sortNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
