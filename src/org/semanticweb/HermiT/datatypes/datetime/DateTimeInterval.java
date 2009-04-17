// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.datatypes.datetime;

import java.util.Collection;

public class DateTimeInterval {
    protected final IntervalType m_intervalType;
    protected final long m_lowerBound;
    protected final BoundType m_lowerBoundType;
    protected final long m_upperBound;
    protected final BoundType m_upperBoundType;
    
    public DateTimeInterval(IntervalType intervalType,long lowerBound,BoundType lowerBoundType,long upperBound,BoundType upperBoundType) {
        assert !isIntervalEmpty(intervalType,lowerBound,lowerBoundType,upperBound,upperBoundType);
        m_intervalType=intervalType;
        m_lowerBound=lowerBound;
        m_lowerBoundType=lowerBoundType;
        m_upperBound=upperBound;
        m_upperBoundType=upperBoundType;
    }
    /**
     * Computes the intersection of this interval with the supplied one. If the two intervals
     * do not intersect, the result is null.
     */
    public DateTimeInterval intersectWith(DateTimeInterval that) {
        if (m_intervalType!=that.m_intervalType)
            return null;
        long newLowerBound;
        BoundType newLowerBoundType;
        if (m_lowerBound<that.m_lowerBound) {
            newLowerBound=that.m_lowerBound;
            newLowerBoundType=that.m_lowerBoundType;
        }
        else if (m_lowerBound>that.m_lowerBound) {
            newLowerBound=m_lowerBound;
            newLowerBoundType=m_lowerBoundType;
        }
        else {
            newLowerBound=m_lowerBound;
            newLowerBoundType=BoundType.getMoreRestrictive(m_lowerBoundType,that.m_lowerBoundType);
        }
        long newUpperBound;
        BoundType newUpperBoundType;
        if (m_upperBound<that.m_upperBound) {
            newUpperBound=m_upperBound;
            newUpperBoundType=m_upperBoundType;
        }
        else if (m_upperBound>that.m_upperBound) {
            newUpperBound=that.m_upperBound;
            newUpperBoundType=that.m_upperBoundType;
        }
        else {
            newUpperBound=m_upperBound;
            newUpperBoundType=BoundType.getMoreRestrictive(m_upperBoundType,that.m_upperBoundType);
        }
        if (isIntervalEmpty(m_intervalType,newLowerBound,newLowerBoundType,newUpperBound,newUpperBoundType))
            return null;
        // The following lines ensure that we don't create a new interval object unless there is need to.
        if (isEqual(m_intervalType,newLowerBound,newLowerBoundType,newUpperBound,newUpperBoundType))
            return this;
        else if (that.isEqual(m_intervalType,newLowerBound,newLowerBoundType,newUpperBound,newUpperBoundType))
            return that;
        else
            return new DateTimeInterval(m_intervalType,newLowerBound,newLowerBoundType,newUpperBound,newUpperBoundType);
    }
    protected boolean isEqual(IntervalType intervalType,long lowerBound,BoundType lowerBoundType,long upperBound,BoundType upperBoundType) {
        return m_intervalType==intervalType && m_lowerBound==lowerBound && m_lowerBoundType==lowerBoundType && m_upperBound==upperBound && m_upperBoundType==upperBoundType;
    }
    /**
     * Subtracts from the given argument the number of distinct objects that are contained in this interval.
     * If the interval contains more objects than argument, the result is zero.
     */
    public int subtractSizeFrom(int argument) {
        if (argument<=0)
            return 0;
        else if (m_lowerBound<m_upperBound) {
            // If the bounds are unequal, the interval contains an infinite number of objects.
            // This is because seconds are decimal numbers in principle.
            return 0;
        }
        else {
            // Since the interval is not empty, both bounds must be inclusive.
            assert m_lowerBoundType==BoundType.INCLUSIVE;
            assert m_upperBoundType==BoundType.INCLUSIVE;
            if (m_intervalType==IntervalType.WITH_TIMEZONE) {
                // There is only one timezoned value that gets mapped to a particular point on the time line.
                return argument-1;
            }
            else {
                // There are 840+840-1 different timezoned values that get mapped to a particular point on the time line.
                // This is because the time zone offset has the form hh:mm where 0<=hh<14 and 0<=mm<60 or 14:00,
                // which gives us 840 values; the other 840 is for the negative time zones, and -1 is so that we don't count 00:00 twice.
                return Math.max(0,argument-(840+840-1));
            }
        }
    }
    public boolean containsDateTime(DateTime dateTime) {
        if (dateTime.hasTimeZoneOffset()) {
            if (m_intervalType==IntervalType.WITHOUT_TIMEZONE)
                return false;
        }
        else {
            if (m_intervalType==IntervalType.WITH_TIMEZONE)
                return false;
        }
        long timeOnTimeline=dateTime.getTimeOnTimeline();
        if (m_lowerBound>timeOnTimeline || (m_lowerBound==timeOnTimeline && m_lowerBoundType==BoundType.EXCLUSIVE))
            return false;
        if (m_upperBound<timeOnTimeline || (m_upperBound==timeOnTimeline && m_upperBoundType==BoundType.EXCLUSIVE))
            return false;
        return true;
    }
    public void enumerateDateTimes(Collection<Object> dateTimes) {
        if (m_lowerBound==m_upperBound) {
            // Since the interval is not empty, both bounds must be inclusive.
            assert m_lowerBoundType==BoundType.INCLUSIVE;
            assert m_upperBoundType==BoundType.INCLUSIVE;
            if (m_intervalType==IntervalType.WITH_TIMEZONE) {
                // There is only one timezoned value that gets mapped to a particular point on the time line.
                dateTimes.add(new DateTime(m_lowerBound,DateTime.NO_TIMEZONE));
            }
            else {
                // There are 840+840-1 different timezoned values that get mapped to a particular point on the time line.
                for (int timeZoneOffset=-840;timeZoneOffset<=840;timeZoneOffset++)
                    dateTimes.add(new DateTime(m_lowerBound,timeZoneOffset));
            }
        }
        else
            throw new IllegalStateException("The data range is infinite.");
    }
    protected static boolean isIntervalEmpty(IntervalType intervalType,long lowerBound,BoundType lowerBoundType,long upperBound,BoundType upperBoundType) {
        return lowerBound>upperBound ||(lowerBound==upperBound && (lowerBoundType==BoundType.EXCLUSIVE || upperBoundType==BoundType.EXCLUSIVE));
    }
    public String toString() {
        StringBuffer buffer=new StringBuffer();
        buffer.append(m_intervalType.toString());
        if (m_lowerBoundType==BoundType.INCLUSIVE)
            buffer.append('[');
        else
            buffer.append('<');
        buffer.append(m_lowerBound);
        buffer.append(" .. ");
        buffer.append(m_upperBound);
        if (m_upperBoundType==BoundType.INCLUSIVE)
            buffer.append(']');
        else
            buffer.append('>');
        return buffer.toString();
    }
}