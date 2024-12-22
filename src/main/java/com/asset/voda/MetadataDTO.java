package com.asset.voda;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MetadataDTO {
    private String object_name;
    private String r_object_type;
    private String cch_contract_no;
    private String cch_sfid;
    private String cch_source;
    private String cch_customer_name;
    private String cch_customer_id;
    private String cch_box_no;
    private String cch_department_code;
    private String deleteflag;
    private String cch_status;
    private String cch_comments;
    private String cch_sub_department_code;
    private List<String> cch_sim_no = new ArrayList<>();
    private List<String> cch_mobile_no = new ArrayList<>();
    private String cch_customer_account;
}