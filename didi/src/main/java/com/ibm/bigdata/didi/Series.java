package com.ibm.bigdata.didi;

import java.util.ArrayList;
import java.util.List;

public class Series {
	
	private String district;
	
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
	
	

}
