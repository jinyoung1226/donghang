package com.ebiz.wsb.domain.token.repository;

import com.ebiz.wsb.domain.token.entity.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {
    BlackList findBlackListByRefreshToken(String refresh_token);
}
