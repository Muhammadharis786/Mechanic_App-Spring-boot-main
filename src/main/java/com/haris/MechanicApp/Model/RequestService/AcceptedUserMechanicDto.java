package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedUserMechanicDto {
    private Long requestId;
    private Long mechanicId;
    private String mechanicName;
    private String mechanicNumber;
    private String mechanicImage;
    private BigDecimal mechanicRating;
    private int mechanicTotalReviews;
    private String mechanicShopName;
    private String mechanicType;
    private int mechanicExperience;
    private Double mechanicLatitude;
    private Double mechanicLongitude;
    private Double distance;
    private String username ;
    private String userimage;
    private String userlocationname;
    private Long userid;
    private String eta;



    public AcceptedUserMechanicDto(Long requestId, Long id, Long userid , String username, String userimgurl, String locationName, String name, String phonenumber, String mechanicimgurl, BigDecimal averageRating, int totalReviews, String shopaddress, String mechanictype, int experienceyears, Double aDouble, Double aDouble1, Double distance, String eta) {
    this.requestId = requestId;
    this.mechanicId = id;
    this.userid = userid;
    this.username = username;
    this.userimage = userimgurl;
    this.userlocationname = locationName;
    this.mechanicName = name;
    this.mechanicNumber = phonenumber;
    this.mechanicImage = mechanicimgurl;
    this.mechanicRating = averageRating;
    this.mechanicTotalReviews = totalReviews;
    this.mechanicShopName = shopaddress;
    this.mechanicType = mechanictype;
    this.mechanicExperience = experienceyears;
    this.distance = distance;
    this.eta = eta;


    }
}
