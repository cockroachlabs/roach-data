package io.roach.data.jpa.department;

public class Address {
    public static class Builder {
        private final Address adddress = new Address();

        public Builder setAddress1(String address1) {
            adddress.address1 = address1;
            return this;
        }

        public Builder setAddress2(String address2) {
            adddress.address2 = address2;
            return this;
        }

        public Builder setCity(String city) {
            adddress.city = city;
            return this;
        }

        public Builder setPostcode(String postcode) {
            adddress.postcode = postcode;
            return this;
        }

        public Builder setCountry(String country) {
            adddress.country = country;
            return this;
        }

        public Address build() {
            return adddress;
        }
    }

    private String address1;

    private String address2;

    private String city;

    private String postcode;

    private String country;

    protected Address() {
    }

    public Address(String address1, String address2, String city, String postcode, String country) {
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.postcode = postcode;
        this.country = country;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getCity() {
        return city;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCountry() {
        return country;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
