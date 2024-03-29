package se.kth.iv1350.kevpos.controller;

import se.kth.iv1350.kevpos.integration.DatabaseUnreachableException;
import se.kth.iv1350.kevpos.model.Register;
import se.kth.iv1350.kevpos.model.Sale;
import se.kth.iv1350.kevpos.integration.SalesLog;
import se.kth.iv1350.kevpos.model.ReceiptDTO;
import se.kth.iv1350.kevpos.model.CheckoutCart;
import se.kth.iv1350.kevpos.integration.InventoryHandler;
import se.kth.iv1350.kevpos.integration.DiscountHandler;
import se.kth.iv1350.kevpos.integration.ItemDTO;
import se.kth.iv1350.kevpos.integration.ItemNotFoundException;
import se.kth.iv1350.kevpos.model.PaymentInfoDTO;
import se.kth.iv1350.kevpos.model.SaleStateDTO;
import se.kth.iv1350.kevpos.util.ExceptionLogger;

/**
 * Handles all calls from the <code>View</code> to <code>the Model</code>. This is the application's only controller. 
 */
public class Controller {
    private final DiscountHandler discountHandler;
    private final InventoryHandler inventoryHandler;
    private final SalesLog salesLog;
    private CheckoutCart checkoutCart;
    private Sale sale;
    private final Register register;
    private final ExceptionLogger exceptionLogger;

    /**
     * Creates an instance of the <code>Controller</code>.
     * @param discountHandler the handler connecting to discount database.
     * @param inventoryHandler the handler connecting to the inventory system
     * @param register the active <code>Register</code> for this point-of-sale.
     * @param salesLog the application's log for completed sales.
     */
    public Controller(DiscountHandler discountHandler, InventoryHandler inventoryHandler, Register register, SalesLog salesLog) {
        this.discountHandler = discountHandler;
        this.inventoryHandler = inventoryHandler;
        this.salesLog = salesLog;
        this.register = register;
        this.exceptionLogger =  new ExceptionLogger();
    }
    
    /**
     * Sets upp a current <code>Sale</code>.
     */
    public void startNewSale() {
        checkoutCart = new CheckoutCart(inventoryHandler);
        sale = new Sale(checkoutCart, discountHandler);
    }
    
    /**
     * Based on the input <code>ItemDTO</code>, adds a corresponding <code>Item</code> to the cart and updates the running total.
     * @param itemRequest a proto-item, all values null except for <code>identifier</code>. 
     * @return Contains relevant info of changed states in the program.
     * @throws ConnectionFailedException thrown when there is some connection error in integration layer.
     * @throws InvalidInputException thrown when an <code>ItemDTO</code>:s <code>identifier</code> does not match
     * an identifier in the inventory database.
     */
    public SaleStateDTO nextItem(ItemDTO itemRequest) throws ConnectionFailedException, InvalidInputException {
        try{
            ItemDTO scannedItem = checkoutCart.addItem(itemRequest);
            SaleStateDTO saleStateDTO = sale.updateRunningTotal(scannedItem);
            return saleStateDTO;
        }
        catch(ItemNotFoundException itemNotFoundException)
        {
            throw new InvalidInputException(itemNotFoundException);
        }
        catch(DatabaseUnreachableException databaseUnreachableException){            
            exceptionLogger.logException(databaseUnreachableException);
            throw new ConnectionFailedException(databaseUnreachableException);
        }
    }
    
    /**
     * Ends the <code>Sale</code> and makes sure all logs are done.
     * @param receivedPayment from the customer.
     * @return receipt with all information about the <code>Sale</code>.
     */
    public ReceiptDTO concludeSale(double receivedPayment) {
        ItemDTO noItem = new ItemDTO(0, 0, null, null, 0, 0);
        SaleStateDTO saleState = sale.updateRunningTotal(noItem);
        PaymentInfoDTO paymentInfo = register.calculateChange(saleState, receivedPayment);

        salesLog.logSale(saleState, paymentInfo);
        ReceiptDTO receipt = new ReceiptDTO(saleState, paymentInfo);
        return receipt;
    }
    
    /**
     * Unimplemented discount
     * @param customerID
     * @return null
     */
    public Sale signalDiscount(int customerID) {
            return null;
    }
}
