/* 
 * Copyright 2017 Idaho National Laboratory.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.inl.SIEVAS.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.inl.SIEVAS.DAO.CriteriaBuilderCriteriaQueryRootTriple;
import gov.inl.SIEVAS.DAO.UserInfoDAO;
import gov.inl.SIEVAS.entity.UserInfo;
import gov.inl.SIEVAS.entity.UserInfo_;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author monejh
 */
public class Utility
{
    
    /***
     * Returns the home url jsp page
     * @return path to the home page
     */
    public static String getHomeURL()
    {
        return "home";
    }
    
    /***
     * Normalizes a getter to be camel case. For example rightName => getRightName
     * @param field The string to normalize
     * @return The normalized getter
     */
    private static String NormalizeGetter(String field)
    {
        return "get"+ field.substring(0,1).toUpperCase() + field.substring(1);
    }
    
    /***
     * Handles the setting up of the order for a SQL query
     * @param sortField The field to sort on.
     * @param sortOrder The order of the sort. If 1, ascending, or -1 for descending
     * @param cb The CritierBuilder object to use
     * @param root The Root object to use
     * @param defaultSortColumn The default sort column if none is given
     * @param objMapper The ObjectMapper class to use
     * @return A list of orders for the query
     */
    public static List<Order> ProcessOrders(String sortField, int sortOrder,CriteriaBuilder cb, Root<?> root, String defaultSortColumn, ObjectMapper objMapper)
    {
        List<Order> orderList = new ArrayList<>();
        if (!sortField.trim().isEmpty())
        {
            
            if (sortOrder>0)
                orderList.add(cb.asc(root.get(sortField.trim())));
            else
                orderList.add(cb.desc(root.get(sortField.trim())));
        }
        if (orderList.isEmpty())
            orderList.add(cb.asc(root.get(defaultSortColumn)));
        
        return orderList;
    }
    
    
    /***
     * Implements the comparison for sorting a list
     * @param <T> The type to compare
     */
    public static class CompareClass<T> implements Comparator<T>
    {
        private String fieldName;
        private int sortOrder;
        private Method method;

        /***
         * Constructor
         * @param cls The class to compare
         * @param fieldName The field name to compare on
         * @param sortOrder The order to use. 1 = ascending, -1 = descending
         * @throws NoSuchMethodException 
         */
        public CompareClass(Class<T> cls, String fieldName, int sortOrder) throws NoSuchMethodException
        {
            this.fieldName = fieldName;
            this.sortOrder = sortOrder;
            method = cls.getMethod(NormalizeGetter(this.fieldName));
            
        }
        
        /***
         * Compares the two objects using the fieldName and sortOrder when initialized
         * @param o1
         * @param o2
         * @return 
         */
        @Override
        public int compare(T o1, T o2)
        {
            try
            {
                if (method.getReturnType().isAssignableFrom(Comparable.class))
                {
                    Comparable obj1 = (Comparable) method.invoke(o1);
                    Comparable obj2 = (Comparable) method.invoke(o2);
                    if (sortOrder>0)
                        return obj1.compareTo(obj2);
                    else
                        return obj2.compareTo(obj1);
                }
                else
                    return -1;
                
            }
            catch (Exception ex)
            {
                Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
                return -1;
            }
            
        }
        
    }
    
    /***
     * Process a raw list of orders for sorting in memory
     * @param <T> The generic
     * @param list THe list of objects to sort
     * @param cls The class type of the object. Should be type T. 
     * @param sortField The sort field to use
     * @param sortOrder The order to use for sorting. 1 = ascending, -1 = descending
     * @param defaultSortColumn The default column to use for sorting
     * @param objMapper The ObjectMapper to use
     */
    public static <T> void ProcessOrders(T[] list, Class<T> cls, String sortField, int sortOrder, String defaultSortColumn, ObjectMapper objMapper)
    {
        List<Order> orderList = new ArrayList<>();
        if (!sortField.trim().isEmpty())
        {
            CompareClass<T> compareClass = null;
            try
            {
                compareClass = new CompareClass<T>(cls, sortField, sortOrder);
            }
            catch(NoSuchMethodException e)
            {
            }
            if (compareClass != null)
                Arrays.sort(list,compareClass);
        }
        else
        {
            CompareClass<T> compareClass = null;
            try
            {
                compareClass = new CompareClass<T>(cls, defaultSortColumn, sortOrder);
            }
            catch(NoSuchMethodException e)
            {
            }
            if (compareClass != null)
                Arrays.sort(list,compareClass);
        }
    }
    
    
    /***
     * Processes the filters based by angular2.
     * @param filters The filter object as json string
     * @param cb The CriteriaBuilder to use
     * @param root The Root to use
     * @param cls The class to use for the object
     * @param objMapper The ObjectMapper to use
     * @return A list of Predicates for running a hibernate query.
     */
    public static List<Predicate> ProcessFilters(String filters, CriteriaBuilder cb, Root<?> root, Class<?> cls, ObjectMapper objMapper)
    {
        List<Predicate> predicateList = new ArrayList<>();
        if (!filters.isEmpty())
        {
            JsonNode node = null;
            try
            {
                 node = objMapper.readTree(filters);
            }
            catch (IOException ex)
            {
                Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            if (node != null)
            {
                String fieldName;
                
                Iterator<String> iter = node.fieldNames();
                while(iter.hasNext() && (fieldName = iter.next()) != null )
                {
                    JsonNode fieldNode = node.get(fieldName);
                    if (fieldNode==null)
                        continue;
                    FilterData filterData = null;
                    try
                    {
                        filterData = objMapper.readValue(fieldNode.toString(), FilterData.class);
                    }
                    catch(IOException e)
                    {
                        Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, e);
                        continue;
                    }
                    
                    if ((filterData!=null) && (!filterData.getValue().isEmpty()))
                    {
                        try
                        {
                            if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Double.class))
                                predicateList.add(cb.equal(root.get(fieldName), Double.parseDouble(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Float.class))
                                predicateList.add(cb.equal(root.get(fieldName), Float.parseFloat(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Long.class))
                                predicateList.add(cb.equal(root.get(fieldName), Long.parseLong(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Integer.class))
                                predicateList.add(cb.equal(root.get(fieldName), Integer.parseInt(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Short.class))
                                predicateList.add(cb.equal(root.get(fieldName), Short.parseShort(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Byte.class))
                                predicateList.add(cb.equal(root.get(fieldName), Byte.parseByte(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(Boolean.class))
                                predicateList.add(cb.equal(root.get(fieldName), Boolean.parseBoolean(filterData.getValue())));
                            else if (cls.getMethod(NormalizeGetter(fieldName)).getReturnType().isAssignableFrom(String.class))
                            {
                                if (filterData.getMatchMode().equals("contains"))
                                    predicateList.add(cb.like(root.get(fieldName), "%" + filterData.getValue() + "%"));
                                else if (filterData.getMatchMode().equals("startsWith"))
                                    predicateList.add(cb.like(root.get(fieldName), filterData.getValue() + "%"));
                                else if (filterData.getMatchMode().equals("endsWith"))
                                    predicateList.add(cb.like(root.get(fieldName), "%" + filterData.getValue()));
                            }
                        }
                        catch(NumberFormatException e)
                        {
                            Logger.getLogger(Utility.class.getName()).log(Level.INFO, null, e);
                        }
                        catch (Exception ex)
                        {
                            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
            }
        }
        return predicateList;
    }
    
    /***
     * Process in memory filters for angular2.
     * @param <T> Object type to filter
     * @param list The list of objects to filter
     * @param filters The filters as a json string
     * @param cls The class type to filter. Should be type T.
     * @param objMapper The ObjectMapper to use.
     * @return A filtered list.
     */
    public static <T> List<T> ProcessFilters(Collection<T> list, String filters, Class<T> cls, ObjectMapper objMapper)
    {
        List<T> returnList = new ArrayList<>();
        for(T item: list)
        {
            boolean match = true;
            if (!filters.isEmpty())
            {
                JsonNode node = null;
                try
                {
                     node = objMapper.readTree(filters);
                }
                catch (IOException ex)
                {
                    Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
                }


                if (node != null)
                {
                    String fieldName;

                    Iterator<String> iter = node.fieldNames();
                    while(iter.hasNext() && (fieldName = iter.next()) != null )
                    {
                        JsonNode fieldNode = node.get(fieldName);
                        if (fieldNode==null)
                            continue;
                        FilterData filterData = null;
                        try
                        {
                            filterData = objMapper.readValue(fieldNode.toString(), FilterData.class);
                        }
                        catch(IOException e)
                        {
                            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, e);
                            continue;
                        }

                        if ((filterData!=null) && (!filterData.getValue().isEmpty()))
                        {
                            match = false;
                            try
                            {
                                Method method = cls.getMethod(NormalizeGetter(fieldName));
                                if (method.getReturnType().isAssignableFrom(String.class))
                                {
                                    if (filterData.getMatchMode().equals("contains"))
                                    {    
                                        String value = (String)method.invoke(item);
                                        if (value.contains(filterData.getValue()))
                                            match = true;
                                        
                                    }
                                    else if (filterData.getMatchMode().equals("startsWith"))
                                    {
                                        String value = (String)method.invoke(item);
                                        if (value.startsWith(filterData.getValue()))
                                            match = true;
                                        
                                    }
                                    else if (filterData.getMatchMode().equals("endsWith"))
                                    {
                                        String value = (String)method.invoke(item);
                                        if (value.endsWith(filterData.getValue()))
                                            match = true;
                                        
                                    }
                                }
                                else if (method.getReturnType().isAssignableFrom(Long.class))
                                    if (Objects.equals(method.invoke(item),Long.parseLong(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Integer.class))
                                    if (Objects.equals(method.invoke(item),Integer.parseInt(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Short.class))
                                    if (Objects.equals(method.invoke(item),Short.parseShort(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Byte.class))
                                    if (Objects.equals(method.invoke(item),Byte.parseByte(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Boolean.class))
                                    if (Objects.equals(method.invoke(item),Boolean.parseBoolean(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Double.class))
                                    if (Objects.equals(method.invoke(item),Double.parseDouble(filterData.getValue())))
                                        match = true;
                                else if (method.getReturnType().isAssignableFrom(Float.class))
                                    if (Objects.equals(method.invoke(item),Float.parseFloat(filterData.getValue())))
                                        match = true;
                                
                                
                            }
                            catch(NumberFormatException e)
                            {
                                Logger.getLogger(Utility.class.getName()).log(Level.INFO, null, e);
                            }
                            catch (Exception ex)
                            {
                                Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else
                            match = true;
                        
                        if (!match)
                            break;
                    }

                }
            }
            else
                match = true;
            if (match)
                returnList.add(item);
        }
        return returnList;
    }
    
    /***
     * Gets the user object by username.
     * @param username THe username to search for.
     * @param userInfoDAO The DAO object to use.
     * @return The user found or null if not found.
     */
    public static UserInfo getUserByUsername(String username, UserInfoDAO userInfoDAO)
    {
        CriteriaBuilderCriteriaQueryRootTriple<UserInfo,UserInfo> triple = userInfoDAO.getCriteriaTriple();
        CriteriaBuilder cb = triple.getCriteriaBuilder();
        Root<UserInfo> root = triple.getRoot();
        
        Predicate pred = cb.equal(root.get(UserInfo_.username), username);
        
        List<UserInfo> list = userInfoDAO.findByCriteria(triple, null, 0, -1, pred);
        if (list.isEmpty())
            return null;
        else
            return list.get(0);
    }
    
    /***
     * Cleans out sensitive user information from the user object.
     * @param user The user object to sanitize
     */
    public static void cleanUserInfo(UserInfo user)
    {
        user.setPassword("");
        user.setPassword2("");
    }
}
