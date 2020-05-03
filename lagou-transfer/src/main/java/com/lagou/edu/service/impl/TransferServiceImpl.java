package com.lagou.edu.service.impl;

import com.lagou.edu.annotationRegister.MyAutowired;
import com.lagou.edu.annotationRegister.MyService;
import com.lagou.edu.annotationRegister.MyTransactional;
import com.lagou.edu.dao.AccountDao;
import com.lagou.edu.pojo.Account;
import com.lagou.edu.service.TransferService;

/**
 * @author 应癫
 */
@MyService(value = "transferService")
@MyTransactional
public class TransferServiceImpl implements TransferService {

    // 最佳状态
    @MyAutowired
    private AccountDao accountDao;

    @Override
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {

            Account from = accountDao.queryAccountByCardNo(fromCardNo);
            Account to = accountDao.queryAccountByCardNo(toCardNo);

            from.setMoney(from.getMoney()-money);
            to.setMoney(to.getMoney()+money);

            accountDao.updateAccountByCardNo(to);
            //int c = 1/0;
            accountDao.updateAccountByCardNo(from);

    }
}
