package com.stable.vo.bus;

import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model_hist2")
public class CodeBaseModelHist extends CodeBaseModel2 {
	private static final long serialVersionUID = 2L;
}
