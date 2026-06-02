package com.haris.MechanicApp.Model.Notification;

public enum NotificationType {
    ROAD_REQUEST,
    APPOINTMENT_REQUEST,
    // Mechanic accepted appointment
    APPOINTMENT_ACCEPTED,

    // Mechanic rejected appointment
    APPOINTMENT_REJECTED,
    MECHANIC_ARRIVED ,
    // User cancelled appointment
    APPOINTMENT_CANCELLED,

    MECHANIC_WORK_STARTED ,
    WORK_COMPLETED ,

    // Mechanic completed work
    APPOINTMENT_COMPLETED,

    APPOINTMENT_EXPIRED,
    APPOINTMENT_REMINDER,

    MECHANIC_ON_THE_WAY,
    // Payment related
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    // Review & Rating
    REVIEW_RECEIVED,

    // General notifications
    GENERAL_NOTIFICATION
}