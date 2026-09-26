package com.ruoyi.system.domain.business;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ruoyi.common.core.domain.BaseEntity;

/** 可提供兑奖、核销服务的线下门店。 */
@JsonIgnoreProperties({ "storeId", "sortNum", "status", "createBy", "createTime", "updateBy", "updateTime", "remark", "params" })
public class BizStore extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long storeId;
    private String id;
    private String name;
    private String city;
    private String district;
    private String address;
    private String phone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String hours;
    private String image;
    private String serviceQr;
    private String groupQr;
    private Integer sortNum;
    private String status;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getHours() { return hours; }
    public void setHours(String hours) { this.hours = hours; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getServiceQr() { return serviceQr; }
    public void setServiceQr(String serviceQr) { this.serviceQr = serviceQr; }
    public String getGroupQr() { return groupQr; }
    public void setGroupQr(String groupQr) { this.groupQr = groupQr; }
    public Integer getSortNum() { return sortNum; }
    public void setSortNum(Integer sortNum) { this.sortNum = sortNum; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
