package com.eyescredit.ec_contact.ec_contact_as;

import java.util.List;

public class Contact {
    /**
     * cpname : 长沙衍数软件科技有限公司
     * mdata : ["15111252840","18684537526","15111252845"]
     * cdata : ["胡琪#法定代表人","肖来#股东","胡琪#总经理"]
     */

    private String cpname;
    private List<String> mdata;
    private List<String> cdata;

    public String getCpname() {
        return cpname;
    }

    public void setCpname(String cpname) {
        this.cpname = cpname;
    }

    public List<String> getMdata() {
        return mdata;
    }

    public void setMdata(List<String> mdata) {
        this.mdata = mdata;
    }

    public List<String> getCdata() {
        return cdata;
    }

    public void setCdata(List<String> cdata) {
        this.cdata = cdata;
    }
}
