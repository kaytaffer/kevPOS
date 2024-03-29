package se.kth.iv1350.kevpos.model;

import java.util.ArrayList;
import se.kth.iv1350.kevpos.integration.DatabaseUnreachableException;
import se.kth.iv1350.kevpos.integration.ItemDTO;
import se.kth.iv1350.kevpos.integration.InventoryHandler;
import se.kth.iv1350.kevpos.integration.ItemNotFoundException;

/**
 * Contains and manages <code>ItemDTO</code>s for the current <code>Sale</code>.
 */
public class CheckoutCart {

    private final InventoryHandler inventoryHandler;
    private boolean firstItemEver;
    private final ArrayList<Item> listOfItems;

    /**
     * Creates an instance of the <code>CheckOutCart</code>
     * @param inventoryHandler the handler of the external inventory system.
     */
    public CheckoutCart(InventoryHandler inventoryHandler) {
            this.inventoryHandler = inventoryHandler;
            this.firstItemEver = true;
            this.listOfItems = new ArrayList<>();
    }
    
    /**
     * Checks if requested <code>Item</code> exists, and if so, adds the <code>Item</code> 
     * to the cart.
     * @param itemRequest a proto-item, all values null except for <code>identifier</code>.
     * @return Either is null, indicating no match in the database, or contains 
     * all data of the requested <code>Item</code>.
     * @throws DatabaseUnreachableException when a database cannot be reached.
     * @throws ItemNotFoundException Thrown when an <code>ItemDTO</code>:s <code>identifier</code> 
     * does not match an identifier in the inventory database.
     */
    public ItemDTO addItem(ItemDTO itemRequest) throws DatabaseUnreachableException, 
            ItemNotFoundException {
        ItemDTO scannedItem = inventoryHandler.constructItemDTO(itemRequest);
        addItemToCheckoutCart(new Item(scannedItem));
        return scannedItem;
    }
    
    /**
     * Get the <code>listOfItems</code> in the current cart.
     * @return the current shopping list.
     */
    final ArrayList<Item> getListOfItems() {
        return listOfItems;
    }
    
    /*  Adds Item to the checkout cart. If it already exists in the cart the 
        quantity increases. */
    private void addItemToCheckoutCart(Item itemBeingAddedToCart) {
        if(firstItemEver){
            listOfItems.add(itemBeingAddedToCart);
            firstItemEver = false;
        }
        else{
            int duplicateItemPosition = findItemPositionInCheckoutCart(itemBeingAddedToCart, 
                    listOfItems);
            if(duplicateItemPosition >= 0)
                incrementItemInCart(itemBeingAddedToCart, duplicateItemPosition);
            else
            listOfItems.add(itemBeingAddedToCart);
        }
    }

    /*  Increases the quantity of Items of a given type in the cart. */
    private void incrementItemInCart(Item itemBeingAddedToCart, int duplicateItemPosition){
        Item combinedItems = listOfItems.get(duplicateItemPosition);
        combinedItems.setQuantity(combinedItems.getQuantity() + itemBeingAddedToCart.getQuantity());        
        listOfItems.set(duplicateItemPosition, combinedItems);
    }

    /*  Goes through listOfItems and checks if there is an Itemof the same type 
        already in the list. That Item's element position is returned, otherwise -1 
        for an Item not found. */
    private int findItemPositionInCheckoutCart(Item itemBeingAddedToCart, 
            ArrayList<Item> listOfItems) throws NullPointerException {
        for(int i = 0; i < listOfItems.size(); i++)
        {
            if(itemBeingAddedToCart.getIdentifier() == listOfItems.get(i).getIdentifier()){
                return i;
            }
        }
        return -1;
    }
}
