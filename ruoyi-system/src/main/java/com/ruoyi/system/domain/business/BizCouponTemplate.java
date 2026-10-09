package com.ruoyi.system.domain.business;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;

/** 优惠券规则定义；前端不参与金额、库存和资格判定。 */
@JsonIgnoreProperties({ "couponTemplateId", "stock", "startTime", "endTime", "sortNum", "status", "createBy", "createTime", "updateBy", "updateTime", "remark", "params" })
public class BizCouponTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long couponTemplateId;
    private String id;
    private String name;
    private String kind;
    private Integer amount;
    private Integer rate;
    private Integer minimum;
    private Integer minQuantity;
    private Integer maxDiscount;
    private Boolean memberOnly;
    private Boolean pointsOnly;
    private String channel;
    private Integer stock;
    private Integer claimLimit;
    private Date startTime;
    private Date endTime;
    private Integer sortNum;
    private String status;

    public Long getCouponTemplateId() { return couponTemplateId; }
    public void setCouponTemplateId(Long couponTemplateId) { this.couponTemplateId = couponTemplateId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getRate() { return rate; }
    public void setRate(Integer rate) { this.rate = rate; }
    public Integer getMinimum() { return minimum; }
    public void setMinimum(Integer minimum) { this.minimum = minimum; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Integer maxDiscount) { this.maxDiscount = maxDiscount; }
    public Boolean getMemberOnly() { return memberOnly; }
    public void setMemberOnly(Boolean memberOnly) { this.memberOnly = memberOnly; }
    public Boolean getPointsOnly() { return pointsOnly; }
    public void setPointsOnly(Boolean pointsOnly) { this.pointsOnly = pointsOnly; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getClaimLimit() { return claimLimit; }
    public void setClaimLimit(Integer claimLimit) { this.claimLimit = claimLimit; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public Integer getSortNum() { return sortNum; }
    public void setSortNum(Integer sortNum) { this.sortNum = sortNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
