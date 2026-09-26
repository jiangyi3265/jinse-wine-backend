package com.ruoyi.system.domain.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;

/** 积分兑换目录。 */
@JsonIgnoreProperties({ "exchangeItemId", "perUserLimit", "sortNum", "status", "createBy", "createTime", "updateBy", "updateTime", "remark", "params" })
public class BizExchangeItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long exchangeItemId;
    private String id;
    private String name;
    private String kind;
    private String templateId;
    private String spec;
    private String image;
    private Integer points;
    private Integer stock;
    private Integer perUserLimit;
    private Integer sortNum;
    private String status;

    public Long getExchangeItemId() { return exchangeItemId; }
    public void setExchangeItemId(Long exchangeItemId) { this.exchangeItemId = exchangeItemId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(Integer perUserLimit) { this.perUserLimit = perUserLimit; }
    public Integer getSortNum() { return sortNum; }
    public void setSortNum(Integer sortNum) { this.sortNum = sortNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
