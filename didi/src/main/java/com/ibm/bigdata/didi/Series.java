package com.ibm.bigdata.didi;

import java.util.ArrayList;
import java.util.List;

public class Series {
	
	private String district;
	private String date;
	
	private List<Slot> data;

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public List<Slot> getData() {
		if (data == null) {
			data = new ArrayList<Slot>();
		}
		return data;
	}

	public void setData(List<Slot> data) {
		this.data = data;
	}
	
	public Series merge(Series s){
		getData().addAll(s.getData());
		this.district = s.getDistrict();
		return s;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

}
