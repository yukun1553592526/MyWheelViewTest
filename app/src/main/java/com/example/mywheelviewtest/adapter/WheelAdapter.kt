package com.example.mywheelviewtest.adapter

interface WheelAdapter<T> {

    /**
     * Gets items count
     * @return the count of @Link{WheelView} items
     */
    fun getItemsCount(): Int

    /**
     * Gets a wheelview item by index
     * @param index the item index
     * @return the wheel item text or null
     */
    fun getItem(index: Int): T

    /**
     * Returns the index of the first occurrence of the specified element in the items of wheelview
     * @param any the item object
     * @return the maximum item length or -1. When -1 been returned,
     * there will be used for the default wheelview width
     */
    fun indexOf(any: T)
}