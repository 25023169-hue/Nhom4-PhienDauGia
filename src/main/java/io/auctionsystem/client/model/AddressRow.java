package io.auctionsystem.client.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AddressRow {
    private final Long id;
    private String receiverName;
    private String phoneNumber;
    private String street;
    private String city;
    private boolean defaultAddress;
    public AddressRow(Long id, String receiverName, String phoneNumber, String addressDetails, boolean defaultAddress) {
        this(id, receiverName, phoneNumber, addressDetails, "", defaultAddress);
    }
    public String getAddressDetails() {
        if (city == null || city.isBlank()) {
            return street;
        }
        return street + ", " + city;
    }

    public String getDefaultText() {
        return defaultAddress ? "Có" : "";
    }
}