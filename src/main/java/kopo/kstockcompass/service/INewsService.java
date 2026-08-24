package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.NewsResponseDto;

import java.util.List;

public interface INewsService {

    /**
     * 종목명(또는 검색어)로 최신 뉴스를 검색하여 반환
     */
    List<NewsResponseDto> searchStockNews(String query);
}