package com.slightsite.app.domain.warehouse;

import java.util.HashMap;
import java.util.Map;

public class Warehouses {
    private int id;
    private int warehouse_id;
    private String title;
    private String address;
    private String phone;
    private int status;
    private String server_product_data;
    private String date_product_request;

    /**
     * Static address for UNDEFINED ID.
     */
    public static final int UNDEFINED_ID = -1;


    public Warehouses(int id, int warehouse_id, String title, String address, String phone, int status) {
        this.id = id;
        this.warehouse_id = warehouse_id;
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.status = status;
    }

    public Warehouses(int warehouse_id, String title, String address, String phone, int status) {
        this(UNDEFINED_ID, warehouse_id, title, address, phone, status);
    }

    public void setWarehouseId(int warehouse_id) {
        this.warehouse_id = warehouse_id;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getWarehouseId() {
        return warehouse_id;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public int getStatus() {
        return status;
    }

    public void setServerProductData(String _data) {
        this.server_product_data = _data;
    }

    public String getServerProductData() {
        return server_product_data;
    }

    public void setDateProductRequest(String _date) {
        this.date_product_request = _date;
    }

    public String getDateProductRequest() {
        return date_product_request;
    }

    /**
     * Returns the status of this Warehouse in Map format.
     * @return the status of this Warehouse in Map format.
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", id + "");
        map.put("warehouse_id", warehouse_id + "");
        map.put("title", title);
        map.put("address", address);
        map.put("phone", phone);
        map.put("status", status + "");
        map.put("server_product_data", server_product_data);
        map.put("date_product_request", date_product_request);

        return map;

    }
}
