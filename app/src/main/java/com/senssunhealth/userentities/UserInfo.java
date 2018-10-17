package com.senssunhealth.userentities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by wucaiyan on 2017/11/14.
 */

public class UserInfo implements Parcelable {
    private String serialNum;
    private String pinId;
    private String sexId;
    private String height;
    private String age;
    private String phys;
    private String weight;

    public UserInfo(String serialNum, String pinId, String sexId, String height, String age, String phys, String weight) {
        this.serialNum = serialNum;
        this.pinId = pinId;
        this.sexId = sexId;
        this.height = height;
        this.age = age;
        this.phys = phys;
        this.weight = weight;
    }

    protected UserInfo(Parcel in) {
        serialNum = in.readString();
        pinId = in.readString();
        sexId = in.readString();
        height = in.readString();
        age = in.readString();
        phys = in.readString();
        weight = in.readString();
    }

    public static final Creator<UserInfo> CREATOR = new Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel in) {
            return new UserInfo(in);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public String getPinId() {
        return pinId;
    }

    public void setPinId(String pinId) {
        this.pinId = pinId;
    }

    public String getSexId() {
        return sexId;
    }

    public void setSexId(String sexId) {
        this.sexId = sexId;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getPhys() {
        return phys;
    }

    public void setPhys(String phys) {
        this.phys = phys;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "serialNum='" + serialNum + '\'' +
                ", pinId='" + pinId + '\'' +
                ", sexId='" + sexId + '\'' +
                ", height='" + height + '\'' +
                ", age='" + age + '\'' +
                ", phys='" + phys + '\'' +
                ", weight='" + weight + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serialNum);
        dest.writeString(pinId);
        dest.writeString(sexId);
        dest.writeString(height);
        dest.writeString(age);
        dest.writeString(phys);
        dest.writeString(weight);
    }
}
