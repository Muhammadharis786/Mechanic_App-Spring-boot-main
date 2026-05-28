package com.haris.MechanicApp.Model.RequestService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MechanicRequestNotificationDto {

    private Long requestId;

    private Long userId;
    private String serviceType;
    private String userNotes;
    private String eta;
    private Double userLatitude;
    private Double userLongitude;
    private String locationName;
    private String userimage ;
    private String username;
    private Double distanceKm;

    private String requestStatus;


    public MechanicRequestNotificationDto(Long requestId, long userid, String serviceType, String userNotes, Double userLatitude, Double userLongitude, String locationName, Double distanceKm, String eta, ServiceRequestStatus requestStatus) {



    }

    public MechanicRequestNotificationDto(Long requestId, long userid, String serviceType, String userNotes, Double userLatitude, Double userLongitude, String locationName, Double distanceKm, String eta, ServiceRequestStatus requestStatus, String username, String userimgurl) {

        this.requestId = requestId;
        this.userId = userid;
        this.serviceType = serviceType;
        this.userNotes = userNotes;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.locationName = locationName;
        this.distanceKm = distanceKm;
        this.eta = eta;
        this.requestStatus = requestStatus.name();
        this.userimage = userimgurl;
        this.username = username;

    }
}