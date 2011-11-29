package ru.mentorbank.backoffice.services.moneytransfer;

import static org.mockito.Mockito.*;
import org.junit.Before;	
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.mockito.ArgumentMatcher;

import ru.mentorbank.backoffice.dao.OperationDao;
import ru.mentorbank.backoffice.dao.exception.OperationDaoException;
import ru.mentorbank.backoffice.dao.stub.OperationDaoStub;
import ru.mentorbank.backoffice.model.Operation;
import ru.mentorbank.backoffice.model.stoplist.JuridicalStopListRequest;
import ru.mentorbank.backoffice.model.stoplist.PhysicalStopListRequest;
import ru.mentorbank.backoffice.model.stoplist.StopListStatus;
import ru.mentorbank.backoffice.model.transfer.JuridicalAccountInfo;
import ru.mentorbank.backoffice.model.transfer.PhysicalAccountInfo;
import ru.mentorbank.backoffice.model.transfer.TransferRequest;
import ru.mentorbank.backoffice.services.accounts.AccountService;
import ru.mentorbank.backoffice.services.moneytransfer.exceptions.TransferException;
import ru.mentorbank.backoffice.services.stoplist.StopListService;
import ru.mentorbank.backoffice.services.stoplist.StopListServiceStub;
import ru.mentorbank.backoffice.test.AbstractSpringTest;

public class MoneyTransferServiceTest extends AbstractSpringTest {

	@Autowired
	private StopListService stopListService;
	private StopListService spyedStopListService;
	private MoneyTransferServiceBean moneyTransferService;
	private JuridicalAccountInfo dstAccount;
	private PhysicalAccountInfo srcAccount;
	private TransferRequest transferRequest; 
	private OperationDao mockedOperationDao;
	private AccountService mockedAccountService;

	@Before
	public void setUp() {
	
		srcAccount = new PhysicalAccountInfo();
        dstAccount = new JuridicalAccountInfo();
	     transferRequest = new TransferRequest();
	     moneyTransferService = new MoneyTransferServiceBean();
	         
	     spyedStopListService = spy(new StopListServiceStub());
	     mockedAccountService = mock(AccountService.class);
	     mockedOperationDao = mock(OperationDao.class);
	     
	     
	     srcAccount.setDocumentSeries(StopListServiceStub.Series_FOR_OK_STATUS);
	     dstAccount.setInn(StopListServiceStub.INN_FOR_OK_STATUS);
	     srcAccount.setAccountNumber("111");
	     dstAccount.setAccountNumber("222");
	     
	     transferRequest.setSrcAccount(srcAccount);
	     transferRequest.setDstAccount(dstAccount);
	     
	     when(mockedAccountService.verifyBalance(srcAccount)).thenReturn(true);
	  		 
	  	((MoneyTransferServiceBean) moneyTransferService).setAccountService(mockedAccountService);
		((MoneyTransferServiceBean) moneyTransferService).setStopListService(spyedStopListService);
		((MoneyTransferServiceBean) moneyTransferService).setOperationDao(mockedOperationDao);

	}

	@Test
	public void transfer() throws TransferException,OperationDaoException {
		   moneyTransferService.transfer(transferRequest);
		// Проверка вызова методов сервиса StopListService
			 verify(spyedStopListService).getJuridicalStopListInfo(argThat(new ArgumentMatcher<JuridicalStopListRequest>() {
				 public boolean matches(Object j) {
					if (j instanceof JuridicalStopListRequest) {
						 JuridicalStopListRequest request = (JuridicalStopListRequest)j;
						 if (request.getInn() == StopListServiceStub.INN_FOR_OK_STATUS)
							 return true;
					}
					return false;
				 }
			 }));
			 
			 verify(spyedStopListService).getPhysicalStopListInfo(argThat(new ArgumentMatcher<PhysicalStopListRequest>() {
				 @Override
				 public boolean matches(Object p) {
					if (p instanceof PhysicalStopListRequest) {
						PhysicalStopListRequest request = (PhysicalStopListRequest)p;
						 if ((request.getDocumentSeries() == StopListServiceStub.Series_FOR_OK_STATUS))
							 return true;
					}
					return false;
				 }
			 }));
			 
			 // Проверка вызова методов сервиса AccountService
			 verify(mockedAccountService).verifyBalance(srcAccount);
			 
			 // Проверка вызова OperationDao.saveOperation() для сохранения операции в таблице операций
			 verify(mockedOperationDao).saveOperation(argThat(new ArgumentMatcher<Operation>(){
				@Override
				public boolean matches(Object op) {
					if (op instanceof Operation) {
						Operation operation = (Operation)op;
						if ((operation.getSrcAccount().getAccountNumber() == "111") &&
								(operation.getDstAccount().getAccountNumber() == "222") &&
								(operation.getSrcStoplistInfo().getStatus() == StopListStatus.OK))
							return true;
					}
					return false;
				}}));
		
	}
}
